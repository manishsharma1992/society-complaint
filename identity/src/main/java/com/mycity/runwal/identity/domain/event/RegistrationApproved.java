package com.mycity.runwal.identity.domain.event;

import com.mycity.runwal.identity.domain.model.user.FlatClaim;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

public record RegistrationApproved(UserId userId, FlatClaim claim, UserId approvedBy, Instant occurredOn) implements DomainEvent {
}
