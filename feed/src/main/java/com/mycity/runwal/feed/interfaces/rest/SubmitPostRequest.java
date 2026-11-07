package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.command.SubmitPostCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param showTowerId the author's tower to show on the post; leave it out to show no tower */
record SubmitPostRequest(@NotBlank @Size(max = 150) String title, @NotBlank @Size(max = 4000) String body,
                         Long showTowerId) {

    SubmitPostCommand toCommand(long societyId) {
        return new SubmitPostCommand(societyId, title, body, showTowerId);
    }
}
