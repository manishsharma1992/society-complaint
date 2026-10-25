package com.mycity.runwal.complaint.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** @param contentType "image/jpeg", "image/png" or "image/webp" */
record PhotoUploadRequest(@NotBlank String contentType, @NotNull @Positive Long sizeBytes) {
}
