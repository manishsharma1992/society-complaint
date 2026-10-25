package com.mycity.runwal.complaint.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param storageKey the key returned with the upload link */
record AttachPhotoRequest(@NotBlank @Size(max = 300) String storageKey) {
}
