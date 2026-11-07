package com.mycity.runwal.complaint.application.query;

/**
 * One staff member's row in the staff report. Counts are over complaints raised in the period and assigned to
 * them; the backlog ({@code openNow}, {@code overdueNow}) is everything assigned to them that is not done yet.
 *
 * @param medianMinutesToStart   from assignment to "work started", or null if nothing was started
 * @param medianMinutesToResolve from assignment to "resolved", or null if nothing was resolved
 * @param resolvedWithinSla      resolved no later than the category's SLA deadline
 */
public record StaffPerformanceView(long staffId, int assigned, int started, int resolved, int rejected,
                                   Long medianMinutesToStart, Long medianMinutesToResolve, int resolvedWithinSla,
                                   int openNow, int overdueNow) {
}
