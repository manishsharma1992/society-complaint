package com.mycity.runwal.feed.application.command;

/** The author has uploaded a photo under {@code uploadKey} and asks to add it to the post. */
public record AttachEvidenceCommand(long societyId, long postId, String uploadKey) {
}
