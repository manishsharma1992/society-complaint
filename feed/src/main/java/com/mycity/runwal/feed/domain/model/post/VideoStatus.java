package com.mycity.runwal.feed.domain.model.post;

/**
 * Processing of an uploaded video: PROCESSING -> READY or FAILED. Only a READY video is ever shown, and only as
 * the server's own re-encoded copy without metadata, never the original upload.
 */
public enum VideoStatus {
    PROCESSING,
    READY,
    FAILED
}
