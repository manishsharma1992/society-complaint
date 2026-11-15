package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A short video documenting a post. The original upload stays private and is never shown: it may carry the
 * phone's location. The server re-encodes it to an MP4 without any metadata plus a poster frame, stored next to
 * the original ({@link #playableKey()}, {@link #posterKey()}), and only those are ever shown, once READY.
 *
 * @param storageKey    the original upload, e.g. "feed/1/42/3f2a....mov"; its extension matches its content type
 * @param facts         the playable copy's size and length; present exactly when READY
 * @param failureReason why processing failed, for the author; present exactly when FAILED
 */
@ValueObject
public record Video(String storageKey, String contentType, long sizeBytes, Instant uploadedAt, VideoStatus status,
                    VideoFacts facts, String failureReason) {

    public static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;
    public static final long MAX_DURATION_MS = 60_000;
    public static final int MAX_PER_POST = 1;
    public static final int MAX_FAILURE_REASON_LENGTH = 300;

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE =
            Map.of("video/mp4", "mp4", "video/quicktime", "mov", "video/webm", "webm");

    public Video {
        Evidence.checkKey(storageKey);
        checkContent(contentType, sizeBytes);
        if (!storageKey.endsWith("." + fileExtensionFor(contentType))) {
            throw new InvalidPostException("The video's file extension does not match its type " + contentType);
        }
        Objects.requireNonNull(uploadedAt, "uploadedAt");
        Objects.requireNonNull(status, "status");
        if ((facts != null) != (status == VideoStatus.READY)) {
            throw new InvalidPostException("A video has its length and size exactly when it is ready");
        }
        if ((failureReason != null) != (status == VideoStatus.FAILED)
                || (failureReason != null && (failureReason.isBlank() || failureReason.length() > MAX_FAILURE_REASON_LENGTH))) {
            throw new InvalidPostException("A failed video needs a short reason, and only a failed one has it");
        }
    }

    /** A video just uploaded under its final key; the server has yet to check and re-encode it. */
    public static Video uploaded(String storageKey, long sizeBytes, Instant at) {
        return new Video(storageKey, contentTypeOf(storageKey), sizeBytes, at, VideoStatus.PROCESSING, null, null);
    }

    Video ready(VideoFacts playable) {
        Objects.requireNonNull(playable, "playable");
        requireProcessing();
        return new Video(storageKey, contentType, sizeBytes, uploadedAt, VideoStatus.READY, playable, null);
    }

    Video failed(String reason) {
        requireProcessing();
        String why = reason == null ? null : reason.strip();
        if (why != null && why.length() > MAX_FAILURE_REASON_LENGTH) {
            why = why.substring(0, MAX_FAILURE_REASON_LENGTH);
        }
        return new Video(storageKey, contentType, sizeBytes, uploadedAt, VideoStatus.FAILED, null, why);
    }

    private void requireProcessing() {
        if (status != VideoStatus.PROCESSING) {
            throw new InvalidPostException("This video has already been processed");
        }
    }

    /** The re-encoded MP4 without metadata; the only copy of the video that is ever shown. */
    public String playableKey() {
        return baseKey() + "-play.mp4";
    }

    /** A frame of the video as a JPEG, shown before it plays. */
    public String posterKey() {
        return baseKey() + "-poster.jpg";
    }

    private String baseKey() {
        return storageKey.substring(0, storageKey.lastIndexOf('.'));
    }

    public static boolean isVideoType(String contentType) {
        return contentType != null && EXTENSION_BY_CONTENT_TYPE.containsKey(contentType);
    }

    public static boolean isVideoKey(String storageKey) {
        return EXTENSION_BY_CONTENT_TYPE.values().stream().anyMatch(ext -> storageKey.endsWith("." + ext));
    }

    /** The content type of a video stored under this key, from its file extension. */
    public static String contentTypeOf(String storageKey) {
        Evidence.checkKey(storageKey);
        return EXTENSION_BY_CONTENT_TYPE.entrySet().stream()
                .filter(e -> storageKey.endsWith("." + e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(Video::unsupportedType);
    }

    static String fileExtensionFor(String contentType) {
        String extension = contentType == null ? null : EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw unsupportedType();
        }
        return extension;
    }

    public static void checkContent(String contentType, long sizeBytes) {
        fileExtensionFor(contentType);
        if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
            throw new EvidenceRejectedException("A video can be at most " + MAX_SIZE_BYTES / (1024 * 1024) + " MB");
        }
    }

    private static EvidenceRejectedException unsupportedType() {
        return new EvidenceRejectedException("A video must be MP4, MOV or WebM");
    }
}
