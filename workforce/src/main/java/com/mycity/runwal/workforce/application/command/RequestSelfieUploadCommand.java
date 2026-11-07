package com.mycity.runwal.workforce.application.command;

/** The logged-in staff member is about to upload a selfie (a JPEG of this size) to clock in or out. */
public record RequestSelfieUploadCommand(long societyId, long sizeBytes) {
}
