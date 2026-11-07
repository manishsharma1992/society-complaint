package com.mycity.runwal.workforce.application.query;

import java.time.Instant;

/** Where to PUT the selfie, with exactly this content type and size, before {@code expiresAt}. */
public record SelfieUploadView(String storageKey, String uploadUrl, String contentType, long sizeBytes,
                               Instant expiresAt) {
}
