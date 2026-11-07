package com.mycity.runwal.feed.domain.event;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

/** @param moderator {@code null} when reports hid the post */
public record PostHidden(PostId postId, UserId moderator, String reason, Instant occurredOn) implements DomainEvent {
}
