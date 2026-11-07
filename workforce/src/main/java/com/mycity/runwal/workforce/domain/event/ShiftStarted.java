package com.mycity.runwal.workforce.domain.event;

import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.model.shift.ShiftId;
import java.time.Instant;

/** A staff member clocked in. */
public record ShiftStarted(ShiftId shiftId, SocietyId societyId, UserId staffId, Instant occurredOn)
        implements DomainEvent {
}
