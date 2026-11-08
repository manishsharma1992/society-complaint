package com.mycity.runwal.complaint.domain.event;

import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Instant;

public record AutoAssignmentSwitched(SocietyId societyId, boolean on, Instant occurredOn) implements DomainEvent {
}
