package com.mycity.runwal.feed.application.query;

/** @param status which posts to list, e.g. PENDING_REVIEW (the default) or HIDDEN */
public record ModerationQueueQuery(long societyId, String status, int page, int size) {
}
