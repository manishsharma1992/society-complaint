package com.mycity.runwal.complaint.application.command;

/** Input to assign a complaint to a staff member. The caller comes from the login token. */
public record AssignComplaintCommand(long societyId, long complaintId, long staffId) {
}
