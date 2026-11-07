package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import java.util.Set;

/**
 * What the server found when it read an uploaded file's headers (not what the uploader claimed).
 *
 * @param contentType      the real type from the file's first bytes (magic bytes): image/jpeg, image/png or
 *                         image/webp; {@code null} when the file is none of these or cannot be read
 * @param leftoverMetadata kinds of embedded metadata still present, e.g. "GPS location"; empty when clean
 */
@ValueObject
public record ImageFacts(String contentType, int widthPx, int heightPx, Set<String> leftoverMetadata) {

    public ImageFacts {
        leftoverMetadata = Set.copyOf(leftoverMetadata);
    }

    public static ImageFacts notAnImage() {
        return new ImageFacts(null, 0, 0, Set.of());
    }
}
