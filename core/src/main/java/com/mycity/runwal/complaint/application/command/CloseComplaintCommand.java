package com.mycity.runwal.complaint.application.command;

/** Input to close a resolved complaint. */
public record CloseComplaintCommand(long societyId, long complaintId) {
}
