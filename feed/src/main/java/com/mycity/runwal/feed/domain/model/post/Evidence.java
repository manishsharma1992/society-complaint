package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * A photo documenting a post. The image lives in object storage; only its key is kept here. The server has read
 * the file itself: its real type matches the key, it is not too large, and it carries no metadata that could
 * identify the author (location, device, XMP, IPTC).
 *
 * @param storageKey e.g. "feed/1/42/3f2a....jpg"; its extension always matches the content type
 */
@ValueObject
public record Evidence(String storageKey, String contentType, long sizeBytes, int widthPx, int heightPx,
                       Instant uploadedAt) {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    public static final long MAX_PIXELS = 50_000_000L;
    public static final int MAX_KEY_LENGTH = 300;

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE =
            Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9/._-]+");

    public Evidence {
        checkKey(storageKey);
        checkContent(contentType, sizeBytes);
        if (!storageKey.endsWith("." + fileExtensionFor(contentType))) {
            throw new InvalidPostException("The photo's file extension does not match its type " + contentType);
        }
        if (widthPx <= 0 || heightPx <= 0 || (long) widthPx * heightPx > MAX_PIXELS) {
            throw new EvidenceRejectedException("A photo can have at most 50 megapixels");
        }
        Objects.requireNonNull(uploadedAt, "uploadedAt");
    }

    /**
     * Evidence from an uploaded file, after the server has read it.
     *
     * @throws EvidenceRejectedException when the file is not what its key says, is too large, or still carries
     *                                   identifying metadata
     */
    public static Evidence inspected(String storageKey, long sizeBytes, ImageFacts facts, Instant at) {
        String claimedType = contentTypeOf(storageKey);
        if (facts.contentType() == null) {
            throw new EvidenceRejectedException("This file is not a JPEG, PNG or WebP photo");
        }
        if (!facts.contentType().equals(claimedType)) {
            throw new EvidenceRejectedException(
                    "This file is really " + facts.contentType() + ", not " + claimedType + " as its name says");
        }
        if (!facts.leftoverMetadata().isEmpty()) {
            throw new EvidenceRejectedException("The photo still contains " + String.join(", ",
                    new TreeSet<>(facts.leftoverMetadata()))
                    + ", which could identify you. Please add it again through the app, which removes this.");
        }
        return new Evidence(storageKey, claimedType, sizeBytes, facts.widthPx(), facts.heightPx(), at);
    }

    /** The content type of a photo stored under this key, from its file extension. */
    public static String contentTypeOf(String storageKey) {
        checkKey(storageKey);
        return EXTENSION_BY_CONTENT_TYPE.entrySet().stream()
                .filter(e -> storageKey.endsWith("." + e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(Evidence::unsupportedType);
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
            throw new EvidenceRejectedException("A photo can be at most " + MAX_SIZE_BYTES / (1024 * 1024) + " MB");
        }
    }

    static void checkKey(String storageKey) {
        if (storageKey == null || storageKey.length() > MAX_KEY_LENGTH
                || !SAFE_KEY.matcher(storageKey).matches() || storageKey.contains("..")) {
            throw new InvalidPostException("Invalid photo storage key");
        }
    }

    private static EvidenceRejectedException unsupportedType() {
        return new EvidenceRejectedException("A photo must be JPEG, PNG or WebP");
    }
}
