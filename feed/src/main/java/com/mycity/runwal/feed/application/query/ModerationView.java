package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.feed.domain.model.post.ModerationSnapshot;
import java.time.Instant;

/** One moderation log entry, without who decided. */
public record ModerationView(String status, Instant at, String reason) {

    public static ModerationView of(ModerationSnapshot entry) {
        return new ModerationView(entry.to().name(), entry.at(), entry.reason());
    }
}
