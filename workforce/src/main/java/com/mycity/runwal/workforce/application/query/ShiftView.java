package com.mycity.runwal.workforce.application.query;

import com.mycity.runwal.workforce.domain.model.shift.Shift;
import java.time.Instant;

/**
 * @param endedAt          null while the staff member is still on duty
 * @param minutesCounted   minutes this shift counts for (capped, see {@link Shift#MAX_COUNTED})
 */
public record ShiftView(long id, long staffId, Instant startedAt, Instant endedAt, long minutesCounted) {

    public static ShiftView of(Shift shift, Instant now) {
        long minutes = shift.workedWithin(shift.startedAt(), Instant.MAX, now).toMinutes();
        return new ShiftView(shift.id().value(), shift.staffId().value(), shift.startedAt(),
                shift.endedAt().orElse(null), minutes);
    }
}
