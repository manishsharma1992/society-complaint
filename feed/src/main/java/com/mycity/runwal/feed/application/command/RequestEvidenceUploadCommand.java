package com.mycity.runwal.feed.application.command;

/** The author asks where to upload a photo for their post (pending review). */
public record RequestEvidenceUploadCommand(long societyId, long postId, String contentType, long sizeBytes) {
}
