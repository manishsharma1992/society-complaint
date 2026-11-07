package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

/**
 * Read-only copy of a moderation log entry, used to read and to rebuild a post's log.
 *
 * @param from      {@code null} for the first entry (the post being submitted)
 * @param moderator who decided; {@code null} when the author submitted the post or reports hid it
 * @param reason    why; required for rejecting, hiding and removing
 */
@ValueObject
public record ModerationSnapshot(int sequence, PostStatus from, PostStatus to, UserId moderator, Instant at,
                                 String reason) {
}
