package com.mycity.runwal.identity.domain.event;

import com.mycity.runwal.identity.domain.model.user.FlatClaim;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

public record RegistrationSubmitted(UserId userId, FlatClaim claim, Instant occurredOn) implements DomainEvent {
}
