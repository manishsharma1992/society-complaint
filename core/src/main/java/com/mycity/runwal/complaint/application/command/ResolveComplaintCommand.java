package com.mycity.runwal.complaint.application.command;

/**
 * Input to mark a complaint as resolved.
 *
 * @param note optional; what was done
 */
public record ResolveComplaintCommand(long societyId, long complaintId, String note) {
}
