package com.mycity.runwal.complaint.application.command;

/** Input to reject a complaint; a reason is required. */
public record RejectComplaintCommand(long societyId, long complaintId, String reason) {
}
