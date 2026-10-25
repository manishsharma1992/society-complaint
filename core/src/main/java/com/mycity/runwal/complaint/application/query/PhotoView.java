package com.mycity.runwal.complaint.application.query;

import com.mycity.runwal.complaint.application.port.PhotoStorage;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintPhoto;
import java.time.Duration;
import java.time.Instant;

/** A photo of a complaint, with a short-lived link to view it. */
public record PhotoView(String url, String contentType, long sizeBytes, long uploadedBy, Instant uploadedAt) {

    /** Long enough to look at a complaint; a fresh link comes with every read. */
    static final Duration VIEW_LINK_VALID_FOR = Duration.ofMinutes(15);

    static PhotoView of(ComplaintPhoto photo, PhotoStorage storage) {
        return new PhotoView(storage.viewUrl(photo.storageKey(), VIEW_LINK_VALID_FOR).toString(),
                photo.contentType(), photo.sizeBytes(), photo.uploadedBy().value(), photo.uploadedAt());
    }
}
