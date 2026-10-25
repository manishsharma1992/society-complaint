package com.mycity.runwal.complaint.application.command;

/** Input to record that work on a complaint has started. */
public record StartWorkCommand(long societyId, long complaintId) {
}
