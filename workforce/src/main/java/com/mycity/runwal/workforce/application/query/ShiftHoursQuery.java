package com.mycity.runwal.workforce.application.query;

import java.time.Instant;

/** Hours worked per staff member in [from, to): every staff member for admins, only themself for staff. */
public record ShiftHoursQuery(long societyId, Instant from, Instant to) {
}
