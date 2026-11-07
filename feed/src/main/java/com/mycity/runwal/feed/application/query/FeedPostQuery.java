package com.mycity.runwal.feed.application.query;

/** One published post of the viewer's feed (their society or a nearby one), by its link. */
public record FeedPostQuery(long societyId, String slug) {
}
