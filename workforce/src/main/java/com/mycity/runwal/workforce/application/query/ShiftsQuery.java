package com.mycity.runwal.workforce.application.query;

import java.time.Instant;

/** Every staff member's shifts in [from, to), newest first, for admins to review. */
public record ShiftsQuery(long societyId, Instant from, Instant to) {
}
