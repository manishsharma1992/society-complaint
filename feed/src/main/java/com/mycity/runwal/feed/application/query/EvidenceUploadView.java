package com.mycity.runwal.feed.application.query;

import java.time.Instant;

/** Where and how to upload one photo: PUT the file to {@code uploadUrl} with this Content-Type before it expires. */
public record EvidenceUploadView(String uploadKey, String uploadUrl, String contentType, long sizeBytes,
                                 Instant expiresAt) {
}
