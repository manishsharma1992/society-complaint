package com.mycity.runwal.complaint.application.query;

import java.time.Instant;

/**
 * How each maintenance staff member did with the complaints raised in [from, to).
 *
 * @param towerId only complaints in this tower, or {@code null} for the whole society (society admin only)
 */
public record StaffPerformanceQuery(long societyId, Instant from, Instant to, Long towerId) {
}
