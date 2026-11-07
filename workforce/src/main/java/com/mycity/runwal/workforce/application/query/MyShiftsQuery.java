package com.mycity.runwal.workforce.application.query;

import java.time.Instant;

/** The logged-in staff member's own shifts in [from, to). */
public record MyShiftsQuery(long societyId, Instant from, Instant to) {
}
