package com.mycity.runwal.feed.domain.event;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Instant;

/** A video was added to a post and waits to be processed. Carries no author. */
public record VideoUploaded(PostId postId, SocietyId societyId, String storageKey, Instant occurredOn)
        implements DomainEvent {
}
