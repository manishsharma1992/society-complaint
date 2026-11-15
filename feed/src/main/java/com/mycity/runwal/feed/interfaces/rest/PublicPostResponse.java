package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.query.EvidenceView;
import com.mycity.runwal.feed.application.query.PublicPostView;
import com.mycity.runwal.feed.application.query.VideoView;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A published post as everyone sees it. The author is always just "a verified resident".
 *
 * @param reactions emoji name -> how many reacted with it; emoji nobody chose are left out
 */
record PublicPostResponse(String slug, String title, String body, String tower, Instant publishedAt,
                          List<Photo> photos, List<Video> videos, Map<String, Integer> reactions) {

    static PublicPostResponse from(PublicPostView view) {
        return new PublicPostResponse(view.slug(), view.title(), view.body(), view.tower(), view.publishedAt(),
                view.evidence().stream().map(Photo::of).toList(), view.videos().stream().map(Video::of).toList(),
                view.reactions());
    }

    /** A short-lived link: every read gives a fresh one. */
    record Photo(String url, int widthPx, int heightPx) {
        static Photo of(EvidenceView view) {
            return new Photo(view.url(), view.widthPx(), view.heightPx());
        }
    }

    /** A ready video: the clean copy and a poster frame, short-lived links like photos. */
    record Video(String url, String posterUrl, int widthPx, int heightPx, long durationMs) {
        static Video of(VideoView view) {
            return new Video(view.url(), view.posterUrl(), view.widthPx(), view.heightPx(), view.durationMs());
        }
    }
}
