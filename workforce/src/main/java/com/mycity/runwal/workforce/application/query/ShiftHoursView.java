package com.mycity.runwal.workforce.application.query;

/**
 * @param shifts          shifts that overlap the period
 * @param minutesWorked   time on duty inside the period
 * @param onDutyNow       whether the staff member has an open shift now
 */
public record ShiftHoursView(long staffId, int shifts, long minutesWorked, boolean onDutyNow) {
}
