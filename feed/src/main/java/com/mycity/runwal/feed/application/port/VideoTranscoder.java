package com.mycity.runwal.feed.application.port;

import com.mycity.runwal.feed.domain.model.post.VideoFacts;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.BiConsumer;

/**
 * Turns an uploaded video into the only copy that is ever shown: checked first, then re-encoded to a widely playable
 * MP4 without any metadata (no location, no device), plus a poster frame.
 */
public interface VideoTranscoder {

    /**
     * Checks and re-encodes the original, hands the outputs to {@code store} (the MP4, then the poster JPEG) and
     * deletes every file it made afterwards, also when something fails.
     *
     * @return the playable copy's size and length
     * @throws com.mycity.runwal.feed.domain.exception.EvidenceRejectedException when the file is not a video that
     *         can be accepted; the message tells the author why. Trying again would not help.
     * @throws RuntimeException any other failure (a tool crashed or took too long); trying again may help
     */
    VideoFacts transcode(InputStream original, BiConsumer<Path, Path> store);
}
