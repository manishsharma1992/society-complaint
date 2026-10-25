package com.mycity.runwal.complaint.application.query;

import com.mycity.runwal.complaint.application.port.PhotoStorage;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.List;

/** Read model of a complaint, in plain types so the interfaces layer never sees the domain. */
public record ComplaintView(
        long id,
        long societyId,
        String scope,
        Long towerId,
        Long flatId,
        long categoryId,
        String title,
        String description,
        String status,
        long raisedBy,
        Long assignedTo,
        Instant raisedAt,
        Instant dueAt,
        List<StatusChangeView> history,
        List<PhotoView> photos,
        List<CommentView> comments) {

    public static ComplaintView of(Complaint complaint, PhotoStorage photoStorage) {
        ComplaintLocation location = complaint.location();
        return new ComplaintView(
                complaint.id().value(),
                complaint.societyId().value(),
                location.scope().name(),
                location.towerId() == null ? null : location.towerId().value(),
                location.flatId() == null ? null : location.flatId().value(),
                complaint.categoryId().value(),
                complaint.title().value(),
                complaint.description().orElse(null),
                complaint.status().name(),
                complaint.raisedBy().value(),
                complaint.assignedTo().map(UserId::value).orElse(null),
                complaint.raisedAt(),
                complaint.dueAt(),
                complaint.history().stream().map(StatusChangeView::of).toList(),
                complaint.photos().stream().map(photo -> PhotoView.of(photo, photoStorage)).toList(),
                complaint.comments().stream().map(CommentView::of).toList());
    }
}
