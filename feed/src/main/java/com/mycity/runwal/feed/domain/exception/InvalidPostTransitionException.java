package com.mycity.runwal.feed.domain.exception;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostStatus;
import com.mycity.runwal.shared.ddd.DomainException;

/** The moderation lifecycle does not allow moving from the current status to the requested one. */
public class InvalidPostTransitionException extends DomainException {

    public InvalidPostTransitionException(PostId postId, PostStatus from, PostStatus to) {
        super("Post " + postId.value() + " cannot move from " + from + " to " + to);
    }
}
