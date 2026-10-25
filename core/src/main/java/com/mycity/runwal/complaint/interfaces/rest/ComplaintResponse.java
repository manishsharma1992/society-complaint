package com.mycity.runwal.complaint.interfaces.rest;

import com.mycity.runwal.complaint.application.query.CommentView;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.application.query.PhotoView;
import com.mycity.runwal.complaint.application.query.StatusChangeView;
import java.time.Instant;
import java.util.List;

record ComplaintResponse(
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
        List<StatusChangeResponse> history,
        List<PhotoResponse> photos,
        List<CommentResponse> comments) {

    static ComplaintResponse from(ComplaintView view) {
        return new ComplaintResponse(view.id(), view.societyId(), view.scope(), view.towerId(), view.flatId(),
                view.categoryId(), view.title(), view.description(), view.status(), view.raisedBy(),
                view.assignedTo(), view.raisedAt(), view.dueAt(),
                view.history().stream().map(StatusChangeResponse::from).toList(),
                view.photos().stream().map(PhotoResponse::from).toList(),
                view.comments().stream().map(CommentResponse::from).toList());
    }

    record StatusChangeResponse(String fromStatus, String toStatus, long changedBy, Instant changedAt, String note) {

        static StatusChangeResponse from(StatusChangeView view) {
            return new StatusChangeResponse(
                    view.fromStatus(), view.toStatus(), view.changedBy(), view.changedAt(), view.note());
        }
    }

    /** @param url a short-lived link to the image; fetch the complaint again for a fresh one */
    record PhotoResponse(String url, String contentType, long sizeBytes, long uploadedBy, Instant uploadedAt) {

        static PhotoResponse from(PhotoView view) {
            return new PhotoResponse(
                    view.url(), view.contentType(), view.sizeBytes(), view.uploadedBy(), view.uploadedAt());
        }
    }

    record CommentResponse(int sequence, long authorId, String text, Instant postedAt) {

        static CommentResponse from(CommentView view) {
            return new CommentResponse(view.sequence(), view.authorId(), view.text(), view.postedAt());
        }
    }
}
