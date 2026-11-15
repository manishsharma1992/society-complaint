package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.query.EvidenceView;
import com.mycity.runwal.feed.application.query.PublicPostView;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A published post as everyone sees it. The author is always just "a verified resident".
 *
 * @param reactions emoji name -> how many reacted with it; emoji nobody chose are left out
 */
record PublicPostResponse(String slug, String title, String body, String tower, Instant publishedAt,
                          List<Photo> photos, Map<String, Integer> reactions) {

    static PublicPostResponse from(PublicPostView view) {
        return new PublicPostResponse(view.slug(), view.title(), view.body(), view.tower(), view.publishedAt(),
                view.evidence().stream().map(Photo::of).toList(), view.reactions());
    }

    /** A short-lived link: every read gives a fresh one. */
    record Photo(String url, int widthPx, int heightPx) {
        static Photo of(EvidenceView view) {
            return new Photo(view.url(), view.widthPx(), view.heightPx());
        }
    }
}
