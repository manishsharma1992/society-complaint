package com.mycity.runwal.feed.domain.event;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Instant;

/** Submitted for review. Carries no author: the author must never appear in a log line. */
public record PostSubmitted(PostId postId, SocietyId societyId, Instant occurredOn) implements DomainEvent {
}
