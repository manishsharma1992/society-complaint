package com.mycity.runwal.feed.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

record EvidenceUploadRequest(@NotBlank String contentType, @Positive long sizeBytes) {
}
