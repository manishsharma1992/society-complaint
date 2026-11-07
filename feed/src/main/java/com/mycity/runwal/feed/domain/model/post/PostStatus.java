package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import java.util.Arrays;

/**
 * Moderation lifecycle of a post: PENDING_REVIEW -> PUBLISHED or REJECTED; PUBLISHED -> HIDDEN;
 * HIDDEN -> PUBLISHED or REMOVED. REJECTED and REMOVED are final.
 */
public enum PostStatus {
    PENDING_REVIEW,
    PUBLISHED,
    REJECTED,
    HIDDEN,
    REMOVED;

    public boolean canMoveTo(PostStatus target) {
        return switch (this) {
            case PENDING_REVIEW -> target == PUBLISHED || target == REJECTED;
            case PUBLISHED -> target == HIDDEN;
            case HIDDEN -> target == PUBLISHED || target == REMOVED;
            case REJECTED, REMOVED -> false;
        };
    }

    public static PostStatus from(String name) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new InvalidPostException(
                        "Unknown post status '" + name + "'; expected one of " + Arrays.toString(values())));
    }
}
