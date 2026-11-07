package com.mycity.runwal.workforce.domain.event;

import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.model.shift.ShiftId;
import java.time.Instant;

/** An admin confirmed, after checking the selfies and locations, that the staff member worked this shift. */
public record ShiftConfirmed(ShiftId shiftId, SocietyId societyId, UserId staffId, UserId reviewer, Instant occurredOn)
        implements DomainEvent {
}
