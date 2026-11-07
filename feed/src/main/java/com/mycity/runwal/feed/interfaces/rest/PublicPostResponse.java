package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.query.PublicPostView;
import java.time.Instant;

/** A published post as everyone sees it. The author is always just "a verified resident". */
record PublicPostResponse(String slug, String title, String body, String tower, Instant publishedAt) {

    static PublicPostResponse from(PublicPostView view) {
        return new PublicPostResponse(view.slug(), view.title(), view.body(), view.tower(), view.publishedAt());
    }
}
