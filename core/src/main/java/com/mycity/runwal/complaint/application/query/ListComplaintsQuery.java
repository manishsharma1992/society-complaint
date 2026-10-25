package com.mycity.runwal.complaint.application.query;

/**
 * @param status     only complaints in this status, or {@code null} for any
 * @param towerId    only complaints in this tower, or {@code null} for any
 * @param assignedTo only complaints assigned to this staff member (their jobs), or {@code null} for anyone's
 */
public record ListComplaintsQuery(long societyId, String status, Long towerId, Long assignedTo, int page, int size) {
}
