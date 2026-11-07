package com.mycity.runwal.feed.domain.event;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.shared.ddd.DomainEvent;
import java.time.Instant;

public record PostReported(PostId postId, int openReports, Instant occurredOn) implements DomainEvent {
}
