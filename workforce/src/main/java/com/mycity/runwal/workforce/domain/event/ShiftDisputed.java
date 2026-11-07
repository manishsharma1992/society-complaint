package com.mycity.runwal.workforce.domain.event;

import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.model.shift.ShiftId;
import java.time.Instant;

/** An admin disputed the shift: the selfies or locations do not show the staff member working it. */
public record ShiftDisputed(ShiftId shiftId, SocietyId societyId, UserId staffId, UserId reviewer, Instant occurredOn)
        implements DomainEvent {
}
