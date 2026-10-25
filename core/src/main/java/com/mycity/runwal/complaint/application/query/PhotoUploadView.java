package com.mycity.runwal.complaint.application.query;

import java.time.Instant;

/** Where and how to upload one photo: PUT the file to {@code uploadUrl} with this Content-Type before it expires. */
public record PhotoUploadView(String storageKey, String uploadUrl, String contentType, long sizeBytes,
                              Instant expiresAt) {
}
