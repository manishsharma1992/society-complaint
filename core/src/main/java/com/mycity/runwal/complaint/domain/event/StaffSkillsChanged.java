package com.mycity.runwal.complaint.domain.event;

import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

public record StaffSkillsChanged(SocietyId societyId, UserId staffId, Instant occurredOn) implements DomainEvent {
}
