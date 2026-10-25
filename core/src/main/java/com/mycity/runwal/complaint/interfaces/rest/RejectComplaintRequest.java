package com.mycity.runwal.complaint.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RejectComplaintRequest(@NotBlank @Size(max = 500) String reason) {
}
