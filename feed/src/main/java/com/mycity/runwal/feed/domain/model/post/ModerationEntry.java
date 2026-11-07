package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.shared.ddd.annotation.DomainEntity;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

/**
 * One entry in a post's append-only moderation log (who, what, when, why), identified by its sequence number
 * within the post. Only reachable through {@link Post}; outside code sees {@link ModerationSnapshot}s.
 */
@DomainEntity
final class ModerationEntry {

    private final int sequence;
    private final PostStatus from;
    private final PostStatus to;
    private final UserId moderator;
    private final Instant at;
    private final String reason;

    ModerationEntry(int sequence, PostStatus from, PostStatus to, UserId moderator, Instant at, String reason) {
        this.sequence = sequence;
        this.from = from;
        this.to = to;
        this.moderator = moderator;
        this.at = at;
        this.reason = reason;
    }

    static ModerationEntry from(ModerationSnapshot snapshot) {
        return new ModerationEntry(snapshot.sequence(), snapshot.from(), snapshot.to(), snapshot.moderator(),
                snapshot.at(), snapshot.reason());
    }

    ModerationSnapshot snapshot() {
        return new ModerationSnapshot(sequence, from, to, moderator, at, reason);
    }
}
