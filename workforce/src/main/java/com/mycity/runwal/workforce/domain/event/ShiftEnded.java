package com.mycity.runwal.workforce.domain.event;

import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.model.shift.ShiftId;
import java.time.Instant;

/** A staff member clocked out. */
public record ShiftEnded(ShiftId shiftId, SocietyId societyId, UserId staffId, Instant occurredOn)
        implements DomainEvent {
}
