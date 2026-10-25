package com.mycity.runwal.identity.domain.event;

import com.mycity.runwal.identity.domain.model.user.RoleAssignment;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

public record RoleRevoked(UserId userId, RoleAssignment role, UserId revokedBy, Instant occurredOn) implements DomainEvent {
}
