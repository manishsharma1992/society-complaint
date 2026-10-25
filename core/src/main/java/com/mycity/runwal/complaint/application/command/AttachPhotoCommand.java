package com.mycity.runwal.complaint.application.command;

/** Input to attach a photo, already uploaded under {@code storageKey}, to its complaint. */
public record AttachPhotoCommand(long societyId, long complaintId, String storageKey) {
}
