package com.mycity.runwal.complaint.application.command;

/** Input to add a message to a complaint. The author comes from the login token. */
public record AddCommentCommand(long societyId, long complaintId, String text) {
}
