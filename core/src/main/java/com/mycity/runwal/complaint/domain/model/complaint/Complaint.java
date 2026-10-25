package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.complaint.domain.event.ComplaintAssigned;
import com.mycity.runwal.complaint.domain.event.ComplaintClosed;
import com.mycity.runwal.complaint.domain.event.ComplaintCommented;
import com.mycity.runwal.complaint.domain.event.ComplaintRaised;
import com.mycity.runwal.complaint.domain.event.ComplaintRejected;
import com.mycity.runwal.complaint.domain.event.ComplaintResolved;
import com.mycity.runwal.complaint.domain.event.ComplaintWorkStarted;
import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.complaint.domain.exception.InvalidStatusTransitionException;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.ddd.AbstractAggregateRoot;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A problem reported by a resident, followed through its lifecycle until it is closed or rejected.
 * Every status change is kept in the history and raises a domain event.
 */
@AggregateRoot
public class Complaint extends AbstractAggregateRoot {

    public static final int MAX_DESCRIPTION_LENGTH = 2000;
    public static final int MAX_NOTE_LENGTH = 500;
    public static final int MAX_PHOTOS = 5;
    public static final int MAX_COMMENTS = 200;

    private final ComplaintId id;
    private final SocietyId societyId;
    private final ComplaintLocation location;
    private final ComplaintCategoryId categoryId;
    private final ComplaintTitle title;
    private final String description;
    private final UserId raisedBy;
    private final Instant raisedAt;
    private final Instant dueAt;
    private final List<StatusChange> history;
    private final List<ComplaintPhoto> photos;
    private final List<ComplaintComment> comments;
    private ComplaintStatus status;
    private UserId assignedTo;

    private Complaint(ComplaintId id, SocietyId societyId, ComplaintLocation location, ComplaintCategoryId categoryId,
                      ComplaintTitle title, String description, UserId raisedBy, Instant raisedAt, Instant dueAt,
                      ComplaintStatus status, UserId assignedTo, List<StatusChange> history,
                      List<ComplaintPhoto> photos, List<ComplaintComment> comments) {
        this.id = Objects.requireNonNull(id, "id");
        this.societyId = Objects.requireNonNull(societyId, "societyId");
        this.location = Objects.requireNonNull(location, "location");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.title = Objects.requireNonNull(title, "title");
        this.description = normalizeDescription(description);
        this.raisedBy = Objects.requireNonNull(raisedBy, "raisedBy");
        this.raisedAt = Objects.requireNonNull(raisedAt, "raisedAt");
        this.dueAt = Objects.requireNonNull(dueAt, "dueAt");
        this.status = Objects.requireNonNull(status, "status");
        this.assignedTo = assignedTo;
        this.history = history;
        this.photos = photos;
        this.comments = comments;
    }

    /** A resident raises a new complaint; it starts OPEN and is due according to the SLA policy. */
    public static Complaint raise(ComplaintId id, SocietyId societyId, ComplaintLocation location,
                                  ComplaintCategory category, ComplaintTitle title, String description,
                                  UserId raisedBy, Instant raisedAt, SlaPolicy slaPolicy) {
        if (!category.belongsTo(societyId)) {
            throw new InvalidComplaintException(
                    "Category " + category.id().value() + " does not belong to society " + societyId.value());
        }
        if (!category.isActive()) {
            throw new InvalidComplaintException("Category '" + category.name() + "' no longer accepts complaints");
        }
        Instant dueAt = slaPolicy.dueAt(category, raisedAt);
        List<StatusChange> history = new ArrayList<>();
        history.add(new StatusChange(1, null, ComplaintStatus.OPEN, raisedBy, raisedAt, null));
        Complaint complaint = new Complaint(id, societyId, location, category.id(), title, description,
                raisedBy, raisedAt, dueAt, ComplaintStatus.OPEN, null, history, new ArrayList<>(), new ArrayList<>());
        complaint.recordEvent(new ComplaintRaised(id, societyId, category.id(), raisedBy, dueAt, raisedAt));
        return complaint;
    }

    /** Rebuilds a stored complaint. Raises no events. */
    public static Complaint reconstitute(ComplaintId id, SocietyId societyId, ComplaintLocation location,
                                         ComplaintCategoryId categoryId, ComplaintTitle title, String description,
                                         UserId raisedBy, Instant raisedAt, Instant dueAt, ComplaintStatus status,
                                         UserId assignedTo, List<StatusChangeSnapshot> history,
                                         List<ComplaintPhoto> photos, List<ComplaintComment> comments) {
        List<StatusChange> entries = new ArrayList<>(history.stream().map(StatusChange::from).toList());
        return new Complaint(id, societyId, location, categoryId, title, description,
                raisedBy, raisedAt, dueAt, status, assignedTo, entries, new ArrayList<>(photos),
                new ArrayList<>(comments));
    }

    public void assignTo(UserId staffId, UserId assignedBy, Instant at) {
        Objects.requireNonNull(staffId, "staffId");
        moveTo(ComplaintStatus.ASSIGNED, assignedBy, at, null);
        this.assignedTo = staffId;
        recordEvent(new ComplaintAssigned(id, staffId, assignedBy, at));
    }

    public void startWork(UserId startedBy, Instant at) {
        moveTo(ComplaintStatus.IN_PROGRESS, startedBy, at, null);
        recordEvent(new ComplaintWorkStarted(id, startedBy, at));
    }

    public void resolve(UserId resolvedBy, Instant at, String resolutionNote) {
        moveTo(ComplaintStatus.RESOLVED, resolvedBy, at, resolutionNote);
        recordEvent(new ComplaintResolved(id, resolvedBy, at));
    }

    public void close(UserId closedBy, Instant at) {
        moveTo(ComplaintStatus.CLOSED, closedBy, at, null);
        recordEvent(new ComplaintClosed(id, closedBy, at));
    }

    public void reject(UserId rejectedBy, Instant at, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidComplaintException("Rejecting a complaint needs a reason");
        }
        moveTo(ComplaintStatus.REJECTED, rejectedBy, at, reason.strip());
        recordEvent(new ComplaintRejected(id, rejectedBy, reason.strip(), at));
    }

    /**
     * Attaches a photo that has already been uploaded to object storage under {@link #photoKeyPrefix()}.
     * Photos can be added until the complaint is closed or rejected.
     */
    public void addPhoto(ComplaintPhoto photo) {
        Objects.requireNonNull(photo, "photo");
        requireRoomForPhoto();
        if (!photo.storageKey().startsWith(photoKeyPrefix())) {
            throw new InvalidComplaintException("The photo was not uploaded for complaint " + id.value());
        }
        if (photos.stream().anyMatch(p -> p.storageKey().equals(photo.storageKey()))) {
            throw new InvalidComplaintException("This photo is already attached");
        }
        photos.add(photo);
    }

    /** Adds a message to the complaint's conversation; closed and rejected complaints take no more comments. */
    public void comment(UserId author, String text, Instant at) {
        if (status == ComplaintStatus.CLOSED || status == ComplaintStatus.REJECTED) {
            throw new InvalidComplaintException("Comments cannot be added to a " + status + " complaint");
        }
        if (comments.size() >= MAX_COMMENTS) {
            throw new InvalidComplaintException("A complaint can have at most " + MAX_COMMENTS + " comments");
        }
        ComplaintComment comment = new ComplaintComment(comments.size() + 1, author, text, at);
        comments.add(comment);
        recordEvent(new ComplaintCommented(id, author, comment.sequence(), at));
    }

    /**
     * Checks that a photo of this type and size can be added now, and returns the storage key to upload it under.
     * Nothing changes until the uploaded photo is attached with {@link #addPhoto}.
     *
     * @param uniqueName a name that is unique for this complaint, e.g. a random UUID
     */
    public String newPhotoKey(String contentType, long sizeBytes, String uniqueName) {
        requireRoomForPhoto();
        ComplaintPhoto.checkContent(contentType, sizeBytes);
        String key = photoKeyPrefix() + uniqueName + "." + ComplaintPhoto.fileExtensionFor(contentType);
        ComplaintPhoto.checkKey(key);
        return key;
    }

    private void requireRoomForPhoto() {
        if (status == ComplaintStatus.CLOSED || status == ComplaintStatus.REJECTED) {
            throw new InvalidComplaintException("Photos cannot be added to a " + status + " complaint");
        }
        if (photos.size() >= MAX_PHOTOS) {
            throw new InvalidComplaintException("A complaint can have at most " + MAX_PHOTOS + " photos");
        }
    }

    /** Every photo of this complaint is stored under this key prefix, so a photo cannot be moved to another complaint. */
    public String photoKeyPrefix() {
        return "complaints/" + societyId.value() + "/" + id.value() + "/";
    }

    private void moveTo(ComplaintStatus target, UserId changedBy, Instant at, String note) {
        Objects.requireNonNull(changedBy, "changedBy");
        Objects.requireNonNull(at, "at");
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new InvalidComplaintException("A status note can have at most " + MAX_NOTE_LENGTH + " characters");
        }
        if (!status.canMoveTo(target)) {
            throw new InvalidStatusTransitionException(id, status, target);
        }
        history.add(new StatusChange(history.size() + 1, status, target, changedBy, at, note));
        status = target;
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String stripped = description.strip();
        if (stripped.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidComplaintException(
                    "A complaint description can have at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return stripped;
    }

    public ComplaintId id() {
        return id;
    }

    public SocietyId societyId() {
        return societyId;
    }

    public ComplaintLocation location() {
        return location;
    }

    public ComplaintCategoryId categoryId() {
        return categoryId;
    }

    public ComplaintTitle title() {
        return title;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public UserId raisedBy() {
        return raisedBy;
    }

    public Instant raisedAt() {
        return raisedAt;
    }

    public Instant dueAt() {
        return dueAt;
    }

    public ComplaintStatus status() {
        return status;
    }

    public Optional<UserId> assignedTo() {
        return Optional.ofNullable(assignedTo);
    }

    /** The attached photos, in the order they were added. */
    public List<ComplaintPhoto> photos() {
        return List.copyOf(photos);
    }

    /** The conversation, oldest first. */
    public List<ComplaintComment> comments() {
        return List.copyOf(comments);
    }

    /** The status history, oldest first. */
    public List<StatusChangeSnapshot> history() {
        return history.stream().map(StatusChange::snapshot).toList();
    }
}
