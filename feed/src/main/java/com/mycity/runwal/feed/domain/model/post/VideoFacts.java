package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;

/** What the server measured in a video it re-encoded: the playable copy's size in pixels and its length. */
@ValueObject
public record VideoFacts(int widthPx, int heightPx, long durationMs) {

    public VideoFacts {
        if (widthPx <= 0 || heightPx <= 0 || durationMs <= 0) {
            throw new EvidenceRejectedException("This file has no playable video");
        }
        if (durationMs > Video.MAX_DURATION_MS) {
            throw new EvidenceRejectedException("A video can be at most " + Video.MAX_DURATION_MS / 1000 + " seconds long");
        }
    }
}
