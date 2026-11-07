package com.mycity.runwal.feed.application.query;

/** Published posts from the viewer's society and the societies near it, newest first. */
public record CommunityFeedQuery(long societyId, int page, int size) {
}
