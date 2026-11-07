package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.feed.domain.event.PostHidden;
import com.mycity.runwal.feed.domain.event.PostPublished;
import com.mycity.runwal.feed.domain.event.PostRejected;
import com.mycity.runwal.feed.domain.event.PostRemoved;
import com.mycity.runwal.feed.domain.event.PostReported;
import com.mycity.runwal.feed.domain.event.PostSubmitted;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.exception.InvalidPostTransitionException;
import com.mycity.runwal.feed.domain.exception.PostingLimitReachedException;
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
 * Documented evidence a verified resident publishes to the society's public feed. Nothing is public until a
 * moderator publishes it. The author is known only inside this context and is never shown.
 * Every status change is kept in the append-only moderation log and raises a domain event.
 */
@AggregateRoot
public class Post extends AbstractAggregateRoot {

    public static final int MAX_POSTS_PER_DAY = 5;
    /** This many reports hide a published post until a moderator reviews it. */
    public static final int REPORTS_TO_HIDE = 3;
    public static final int MAX_REASON_LENGTH = 500;
    public static final int MAX_EVIDENCE = 5;

    private final PostId id;
    private final SocietyId societyId;
    private final PostSlug slug;
    private final UserId author;
    private final PostContent content;
    private final ShownTower shownTower;
    private final Instant submittedAt;
    private final List<ModerationEntry> moderationLog;
    private final List<PostReport> openReports;
    private final List<Evidence> evidence;
    private PostStatus status;
    private Instant publishedAt;

    private Post(PostId id, SocietyId societyId, PostSlug slug, UserId author, PostContent content,
                 ShownTower shownTower, Instant submittedAt, PostStatus status, Instant publishedAt,
                 List<ModerationEntry> moderationLog, List<PostReport> openReports, List<Evidence> evidence) {
        this.id = Objects.requireNonNull(id, "id");
        this.societyId = Objects.requireNonNull(societyId, "societyId");
        this.slug = Objects.requireNonNull(slug, "slug");
        this.author = Objects.requireNonNull(author, "author");
        this.content = Objects.requireNonNull(content, "content");
        this.shownTower = shownTower;
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
        this.status = Objects.requireNonNull(status, "status");
        this.publishedAt = publishedAt;
        this.moderationLog = moderationLog;
        this.openReports = openReports;
        this.evidence = evidence;
    }

    /**
     * A verified resident submits a post; it waits for review.
     *
     * @param shownTower        the author's tower if they chose to show it, else {@code null}
     * @param postsInLastDay    how many posts the author submitted in the 24 hours before {@code at}
     */
    public static Post submit(PostId id, SocietyId societyId, PostSlug slug, UserId author, PostContent content,
                              ShownTower shownTower, Instant at, int postsInLastDay) {
        if (postsInLastDay >= MAX_POSTS_PER_DAY) {
            throw new PostingLimitReachedException(MAX_POSTS_PER_DAY);
        }
        List<ModerationEntry> log = new ArrayList<>();
        log.add(new ModerationEntry(1, null, PostStatus.PENDING_REVIEW, null, at, null));
        Post post = new Post(id, societyId, slug, author, content, shownTower, at, PostStatus.PENDING_REVIEW,
                null, log, new ArrayList<>(), new ArrayList<>());
        post.recordEvent(new PostSubmitted(id, societyId, at));
        return post;
    }

    /** Rebuilds a stored post. Raises no events. */
    public static Post reconstitute(PostId id, SocietyId societyId, PostSlug slug, UserId author,
                                    PostContent content, ShownTower shownTower, Instant submittedAt,
                                    PostStatus status, Instant publishedAt, List<ModerationSnapshot> moderationLog,
                                    List<PostReport> openReports, List<Evidence> evidence) {
        return new Post(id, societyId, slug, author, content, shownTower, submittedAt, status, publishedAt,
                new ArrayList<>(moderationLog.stream().map(ModerationEntry::from).toList()),
                new ArrayList<>(openReports), new ArrayList<>(evidence));
    }

    /** A moderator publishes a pending post, or restores a hidden one; open reports count as reviewed. */
    public void publish(UserId moderator, Instant at) {
        Objects.requireNonNull(moderator, "moderator");
        moveTo(PostStatus.PUBLISHED, moderator, at, null);
        if (publishedAt == null) {
            publishedAt = at;
        }
        openReports.clear();
        recordEvent(new PostPublished(id, moderator, at));
    }

    public void reject(UserId moderator, Instant at, String reason) {
        Objects.requireNonNull(moderator, "moderator");
        String why = requireReason(reason, "Rejecting");
        moveTo(PostStatus.REJECTED, moderator, at, why);
        recordEvent(new PostRejected(id, moderator, why, at));
    }

    public void hide(UserId moderator, Instant at, String reason) {
        Objects.requireNonNull(moderator, "moderator");
        String why = requireReason(reason, "Hiding");
        moveTo(PostStatus.HIDDEN, moderator, at, why);
        recordEvent(new PostHidden(id, moderator, why, at));
    }

    public void remove(UserId moderator, Instant at, String reason) {
        Objects.requireNonNull(moderator, "moderator");
        String why = requireReason(reason, "Removing");
        moveTo(PostStatus.REMOVED, moderator, at, why);
        recordEvent(new PostRemoved(id, moderator, why, at));
    }

    /**
     * Anyone reports a published post. The {@value #REPORTS_TO_HIDE}th report hides it until a moderator
     * reviews it. Each reporter can report a post once.
     */
    public void report(PostReport report) {
        Objects.requireNonNull(report, "report");
        if (status != PostStatus.PUBLISHED) {
            throw new InvalidPostException("Only published posts can be reported");
        }
        if (openReports.stream().anyMatch(r -> r.reporterKey().equals(report.reporterKey()))) {
            throw new InvalidPostException("You have already reported this post");
        }
        openReports.add(report);
        recordEvent(new PostReported(id, openReports.size(), report.reportedAt()));
        if (openReports.size() >= REPORTS_TO_HIDE) {
            String why = openReports.size() + " reports";
            moveTo(PostStatus.HIDDEN, null, report.reportedAt(), why);
            recordEvent(new PostHidden(id, null, why, report.reportedAt()));
        }
    }

    /**
     * Checks that the post can take a photo of this type and size now, and returns the key to upload it under.
     * Uploads go to a key of their own: {@link #evidenceKeyFor} later moves the file to a key the uploader has
     * no link for, so a checked photo cannot be swapped afterwards.
     *
     * @param uniqueName a name that is unique for this post, e.g. a random UUID
     */
    public String newUploadKey(String contentType, long sizeBytes, String uniqueName) {
        requireRoomForEvidence();
        Evidence.checkContent(contentType, sizeBytes);
        String key = uploadKeyPrefix() + uniqueName + "." + Evidence.fileExtensionFor(contentType);
        Evidence.checkKey(key);
        return key;
    }

    /** Where an upload of this post is kept once checked; refuses uploads made for another post. */
    public String evidenceKeyFor(String uploadKey, String uniqueName) {
        Evidence.checkKey(uploadKey);
        if (!uploadKey.startsWith(uploadKeyPrefix())) {
            throw new InvalidPostException("The photo was not uploaded for this post");
        }
        String key = evidenceKeyPrefix() + uniqueName + "." + Evidence.fileExtensionFor(Evidence.contentTypeOf(uploadKey));
        Evidence.checkKey(key);
        return key;
    }

    /** Adds a checked photo; photos can be added only while the post waits for review. */
    public void addEvidence(Evidence photo) {
        Objects.requireNonNull(photo, "photo");
        requireRoomForEvidence();
        String key = photo.storageKey();
        if (!key.startsWith(evidenceKeyPrefix()) || key.startsWith(uploadKeyPrefix())) {
            throw new InvalidPostException("The photo does not belong to this post");
        }
        if (evidence.stream().anyMatch(e -> e.storageKey().equals(key))) {
            throw new InvalidPostException("This photo is already attached");
        }
        evidence.add(photo);
    }

    private void requireRoomForEvidence() {
        if (status != PostStatus.PENDING_REVIEW) {
            throw new InvalidPostException("Photos can only be added while the post waits for review");
        }
        if (evidence.size() >= MAX_EVIDENCE) {
            throw new InvalidPostException("A post can have at most " + MAX_EVIDENCE + " photos");
        }
    }

    /** Every checked photo of this post is stored under this prefix. */
    public String evidenceKeyPrefix() {
        return "feed/" + societyId.value() + "/" + id.value() + "/";
    }

    private String uploadKeyPrefix() {
        return evidenceKeyPrefix() + "upload/";
    }

    public boolean isWrittenBy(UserId user) {
        return author.equals(user);
    }

    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }

    private void moveTo(PostStatus target, UserId moderator, Instant at, String reason) {
        Objects.requireNonNull(at, "at");
        if (!status.canMoveTo(target)) {
            throw new InvalidPostTransitionException(id, status, target);
        }
        moderationLog.add(new ModerationEntry(moderationLog.size() + 1, status, target, moderator, at, reason));
        status = target;
    }

    private static String requireReason(String reason, String action) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidPostException(action + " a post needs a reason");
        }
        String stripped = reason.strip();
        if (stripped.length() > MAX_REASON_LENGTH) {
            throw new InvalidPostException("A reason can have at most " + MAX_REASON_LENGTH + " characters");
        }
        return stripped;
    }

    public PostId id() {
        return id;
    }

    public SocietyId societyId() {
        return societyId;
    }

    public PostSlug slug() {
        return slug;
    }

    /** Who wrote the post. Never shown outside this context, except to a society admin (audited). */
    public UserId author() {
        return author;
    }

    public PostContent content() {
        return content;
    }

    public Optional<ShownTower> shownTower() {
        return Optional.ofNullable(shownTower);
    }

    public Instant submittedAt() {
        return submittedAt;
    }

    public PostStatus status() {
        return status;
    }

    /** When the post first became public. */
    public Optional<Instant> publishedAt() {
        return Optional.ofNullable(publishedAt);
    }

    /** The moderation log, oldest first. */
    public List<ModerationSnapshot> moderationLog() {
        return moderationLog.stream().map(ModerationEntry::snapshot).toList();
    }

    /** The photos, in the order they were added. */
    public List<Evidence> evidence() {
        return List.copyOf(evidence);
    }

    /** Reports since the post was last published (reviewed). */
    public List<PostReport> openReports() {
        return List.copyOf(openReports);
    }
}
