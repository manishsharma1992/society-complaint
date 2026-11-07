package com.mycity.runwal.workforce.application.query;

import com.mycity.runwal.workforce.application.port.SelfieStorage;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import java.time.Instant;

/**
 * @param endedAt          null while the staff member is still on duty
 * @param minutesCounted   minutes this shift counts for (capped, see {@link Shift#MAX_COUNTED})
 * @param start            null for shifts recorded before evidence was required
 * @param end              null while open (or for shifts recorded before evidence was required)
 * @param review           null until an admin reviews the ended shift
 * @param needsAttention   clocked in or out outside the site, without a location, or without evidence
 */
public record ShiftView(long id, long staffId, Instant startedAt, Instant endedAt, long minutesCounted,
                        ClockEvidenceView start, ClockEvidenceView end, ShiftReviewView review,
                        boolean needsAttention) {

    public static ShiftView of(Shift shift, Instant now, SelfieStorage storage) {
        long minutes = shift.workedWithin(shift.startedAt(), Instant.MAX, now).toMinutes();
        return new ShiftView(shift.id().value(), shift.staffId().value(), shift.startedAt(),
                shift.endedAt().orElse(null), minutes,
                shift.startEvidence().map(e -> ClockEvidenceView.of(e, storage)).orElse(null),
                shift.endEvidence().map(e -> ClockEvidenceView.of(e, storage)).orElse(null),
                shift.review().map(ShiftReviewView::of).orElse(null),
                shift.needsAttention());
    }
}
