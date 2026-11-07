package com.mycity.runwal.feed.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record AttachEvidenceRequest(@NotBlank @Size(max = 300) String uploadKey) {
}
