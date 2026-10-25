package com.mycity.runwal.residency.domain.event;

import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

public record FlatMemberRemoved(FlatId flatId, SocietyId societyId, UserId userId, UserId removedBy,
                                Instant occurredOn) implements DomainEvent {
}
