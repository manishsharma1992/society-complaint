package com.mycity.runwal.complaint.application.command;

/** Input to get a link for uploading one photo of a complaint. */
public record RequestPhotoUploadCommand(long societyId, long complaintId, String contentType, long sizeBytes) {
}
