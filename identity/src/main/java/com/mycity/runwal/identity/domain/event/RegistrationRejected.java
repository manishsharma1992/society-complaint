package com.mycity.runwal.identity.domain.event;

import com.mycity.runwal.identity.domain.model.user.FlatClaim;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

public record RegistrationRejected(UserId userId, FlatClaim claim, String reason, UserId rejectedBy, Instant occurredOn) implements DomainEvent {
}
