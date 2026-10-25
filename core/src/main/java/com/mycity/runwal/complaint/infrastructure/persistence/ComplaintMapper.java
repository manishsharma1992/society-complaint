package com.mycity.runwal.complaint.infrastructure.persistence;

import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintComment;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintPhoto;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.complaint.domain.model.complaint.StatusChangeSnapshot;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;

/** Maps the Complaint aggregate to and from its JPA entities. */
final class ComplaintMapper {

    private ComplaintMapper() {
    }

    static ComplaintJpaEntity toNewEntity(Complaint complaint) {
        ComplaintJpaEntity entity = new ComplaintJpaEntity();
        entity.id = complaint.id().value();
        entity.societyId = complaint.societyId().value();
        entity.scope = complaint.location().scope();
        entity.towerId = complaint.location().towerId() == null ? null : complaint.location().towerId().value();
        entity.flatId = complaint.location().flatId() == null ? null : complaint.location().flatId().value();
        entity.categoryId = complaint.categoryId().value();
        entity.title = complaint.title().value();
        entity.description = complaint.description().orElse(null);
        entity.raisedBy = complaint.raisedBy().value();
        entity.raisedAt = complaint.raisedAt();
        entity.dueAt = complaint.dueAt();
        updateEntity(complaint, entity);
        return entity;
    }

    /** Copies the parts of a complaint that change over its lifecycle; history, photos and comments are only appended. */
    static void updateEntity(Complaint complaint, ComplaintJpaEntity entity) {
        entity.status = complaint.status();
        entity.assignedTo = complaint.assignedTo().map(UserId::value).orElse(null);
        List<StatusChangeSnapshot> history = complaint.history();
        for (StatusChangeSnapshot change : history.subList(entity.history.size(), history.size())) {
            entity.history.add(toEntity(change));
        }
        List<ComplaintPhoto> photos = complaint.photos();
        for (int i = entity.photos.size(); i < photos.size(); i++) {
            entity.photos.add(toEntity(photos.get(i), i + 1));
        }
        List<ComplaintComment> comments = complaint.comments();
        for (ComplaintComment comment : comments.subList(entity.comments.size(), comments.size())) {
            entity.comments.add(toEntity(comment));
        }
    }

    static Complaint toDomain(ComplaintJpaEntity entity) {
        return Complaint.reconstitute(
                new ComplaintId(entity.id),
                new SocietyId(entity.societyId),
                new ComplaintLocation(entity.scope,
                        entity.towerId == null ? null : new TowerId(entity.towerId),
                        entity.flatId == null ? null : new FlatId(entity.flatId)),
                new ComplaintCategoryId(entity.categoryId),
                new ComplaintTitle(entity.title),
                entity.description,
                new UserId(entity.raisedBy),
                entity.raisedAt,
                entity.dueAt,
                entity.status,
                entity.assignedTo == null ? null : new UserId(entity.assignedTo),
                entity.history.stream().map(ComplaintMapper::toSnapshot).toList(),
                entity.photos.stream().map(ComplaintMapper::toPhoto).toList(),
                entity.comments.stream().map(ComplaintMapper::toComment).toList());
    }

    private static ComplaintStatusChangeJpaEntity toEntity(StatusChangeSnapshot change) {
        ComplaintStatusChangeJpaEntity entity = new ComplaintStatusChangeJpaEntity();
        entity.sequenceNo = change.sequence();
        entity.fromStatus = change.from();
        entity.toStatus = change.to();
        entity.changedBy = change.changedBy().value();
        entity.changedAt = change.changedAt();
        entity.note = change.note();
        return entity;
    }

    private static ComplaintPhotoJpaEntity toEntity(ComplaintPhoto photo, int position) {
        ComplaintPhotoJpaEntity entity = new ComplaintPhotoJpaEntity();
        entity.position = position;
        entity.storageKey = photo.storageKey();
        entity.contentType = photo.contentType();
        entity.sizeBytes = photo.sizeBytes();
        entity.uploadedBy = photo.uploadedBy().value();
        entity.uploadedAt = photo.uploadedAt();
        return entity;
    }

    private static ComplaintCommentJpaEntity toEntity(ComplaintComment comment) {
        ComplaintCommentJpaEntity entity = new ComplaintCommentJpaEntity();
        entity.sequenceNo = comment.sequence();
        entity.authorId = comment.author().value();
        entity.text = comment.text();
        entity.postedAt = comment.postedAt();
        return entity;
    }

    private static ComplaintComment toComment(ComplaintCommentJpaEntity entity) {
        return new ComplaintComment(entity.sequenceNo, new UserId(entity.authorId), entity.text, entity.postedAt);
    }

    private static ComplaintPhoto toPhoto(ComplaintPhotoJpaEntity entity) {
        return new ComplaintPhoto(entity.storageKey, entity.contentType, entity.sizeBytes,
                new UserId(entity.uploadedBy), entity.uploadedAt);
    }

    private static StatusChangeSnapshot toSnapshot(ComplaintStatusChangeJpaEntity entity) {
        return new StatusChangeSnapshot(entity.sequenceNo, entity.fromStatus, entity.toStatus,
                new UserId(entity.changedBy), entity.changedAt, entity.note);
    }
}
