package com.mycity.runwal.feed.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The emoji to react with: THUMBS_UP, HEART, SURPRISED, SAD or ANGRY. */
record ReactionRequest(@NotBlank @Size(max = 20) String emoji) {
}
