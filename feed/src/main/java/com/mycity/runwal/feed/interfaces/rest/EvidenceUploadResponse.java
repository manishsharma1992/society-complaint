package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.query.EvidenceUploadView;
import java.time.Instant;

record EvidenceUploadResponse(String uploadKey, String uploadUrl, String contentType, long sizeBytes,
                              Instant expiresAt) {

    static EvidenceUploadResponse from(EvidenceUploadView view) {
        return new EvidenceUploadResponse(view.uploadKey(), view.uploadUrl(), view.contentType(), view.sizeBytes(),
                view.expiresAt());
    }
}
