package com.mycity.runwal.complaint.interfaces.rest;

import com.mycity.runwal.complaint.application.query.PhotoUploadView;
import java.time.Instant;
import java.util.Map;

/** Tells the client exactly how to upload: {@code method} the file to {@code uploadUrl} with {@code headers}. */
record PhotoUploadResponse(String storageKey, String uploadUrl, String method, Map<String, String> headers,
                           Instant expiresAt) {

    static PhotoUploadResponse from(PhotoUploadView view) {
        return new PhotoUploadResponse(view.storageKey(), view.uploadUrl(), "PUT",
                Map.of("Content-Type", view.contentType()), view.expiresAt());
    }
}
