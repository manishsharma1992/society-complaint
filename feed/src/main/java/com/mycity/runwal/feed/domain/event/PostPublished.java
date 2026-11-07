package com.mycity.runwal.feed.domain.event;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

public record PostPublished(PostId postId, UserId moderator, Instant occurredOn) implements DomainEvent {
}
