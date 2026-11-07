package com.mycity.runwal.feed.application.command;

/**
 * A moderator decides on a post.
 *
 * @param reason required for REJECT, HIDE and REMOVE
 */
public record ModeratePostCommand(long societyId, long postId, Decision decision, String reason) {

    public enum Decision {
        PUBLISH,
        REJECT,
        HIDE,
        REMOVE
    }
}
