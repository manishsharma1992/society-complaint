package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.query.EvidenceView;
import com.mycity.runwal.feed.application.query.ModerationView;
import com.mycity.runwal.feed.application.query.PostView;
import com.mycity.runwal.feed.application.query.ReportView;
import java.time.Instant;
import java.util.List;

/** A post with its moderation state, for its author and for moderators. Never contains the author. */
record PostResponse(long id, String slug, String title, String body, String tower, String status,
                    Instant submittedAt, Instant publishedAt, List<Photo> evidence, List<Moderation> moderation,
                    List<Report> reports) {

    /** @param url a link that works for 15 minutes; every read gives a fresh one */
    record Photo(String url, String contentType, int widthPx, int heightPx) {

        static Photo from(EvidenceView view) {
            return new Photo(view.url(), view.contentType(), view.widthPx(), view.heightPx());
        }
    }

    record Moderation(String status, Instant at, String reason) {

        static Moderation from(ModerationView view) {
            return new Moderation(view.status(), view.at(), view.reason());
        }
    }

    record Report(String reason, Instant reportedAt) {

        static Report from(ReportView view) {
            return new Report(view.reason(), view.reportedAt());
        }
    }

    static PostResponse from(PostView view) {
        return new PostResponse(view.id(), view.slug(), view.title(), view.body(), view.tower(), view.status(),
                view.submittedAt(), view.publishedAt(), view.evidence().stream().map(Photo::from).toList(),
                view.moderation().stream().map(Moderation::from).toList(),
                view.reports().stream().map(Report::from).toList());
    }
}
