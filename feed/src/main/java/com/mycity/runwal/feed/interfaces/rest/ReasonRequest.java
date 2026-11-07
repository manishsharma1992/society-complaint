package com.mycity.runwal.feed.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Why: a report, or a moderator's reason to reject, hide or remove. */
record ReasonRequest(@NotBlank @Size(max = 500) String reason) {
}
