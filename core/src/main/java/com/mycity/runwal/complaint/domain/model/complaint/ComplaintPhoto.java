package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A photo attached to a complaint. The image itself lives in object storage; only its key is kept here.
 * The key's file extension always matches the content type, so the type can be read back from the key.
 *
 * @param storageKey the object's key in the bucket, e.g. "complaints/1/42/3f2a....jpg"
 */
@ValueObject
public record ComplaintPhoto(String storageKey, String contentType, long sizeBytes, UserId uploadedBy,
                             Instant uploadedAt) {

    public static final int MAX_KEY_LENGTH = 300;
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE =
            Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    public static final Set<String> ALLOWED_CONTENT_TYPES = EXTENSION_BY_CONTENT_TYPE.keySet();

    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9/._-]+");

    public ComplaintPhoto {
        checkKey(storageKey);
        checkContent(contentType, sizeBytes);
        if (!storageKey.endsWith("." + fileExtensionFor(contentType))) {
            throw new InvalidComplaintException("The photo's file extension does not match its type " + contentType);
        }
        Objects.requireNonNull(uploadedBy, "uploadedBy");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
    }

    /** The content type of a photo stored under this key, from its file extension. */
    public static String contentTypeOf(String storageKey) {
        checkKey(storageKey);
        return EXTENSION_BY_CONTENT_TYPE.entrySet().stream()
                .filter(e -> storageKey.endsWith("." + e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(ComplaintPhoto::unsupportedType);
    }

    static String fileExtensionFor(String contentType) {
        String extension = contentType == null ? null : EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw unsupportedType();
        }
        return extension;
    }

    static void checkContent(String contentType, long sizeBytes) {
        fileExtensionFor(contentType);
        if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
            throw new InvalidComplaintException("A photo can be at most " + MAX_SIZE_BYTES / (1024 * 1024) + " MB");
        }
    }

    static void checkKey(String storageKey) {
        if (storageKey == null || storageKey.length() > MAX_KEY_LENGTH
                || !SAFE_KEY.matcher(storageKey).matches() || storageKey.contains("..")) {
            throw new InvalidComplaintException("Invalid photo storage key");
        }
    }

    private static InvalidComplaintException unsupportedType() {
        return new InvalidComplaintException("A photo must be JPEG, PNG or WebP");
    }
}
