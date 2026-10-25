package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.port.PhotoStorage;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

/** In-memory bucket: {@link #upload} stands in for the client's PUT to the upload link. */
final class FakePhotoStorage implements PhotoStorage {

    final Map<String, Long> objects = new HashMap<>();

    void upload(String storageKey, long sizeBytes) {
        objects.put(storageKey, sizeBytes);
    }

    @Override
    public URI uploadUrl(String storageKey, String contentType, long sizeBytes, Duration validFor) {
        return URI.create("https://bucket.test/" + storageKey + "?upload&expires=" + validFor.toSeconds());
    }

    @Override
    public OptionalLong sizeOf(String storageKey) {
        Long size = objects.get(storageKey);
        return size == null ? OptionalLong.empty() : OptionalLong.of(size);
    }

    @Override
    public URI viewUrl(String storageKey, Duration validFor) {
        return URI.create("https://bucket.test/" + storageKey + "?view&expires=" + validFor.toSeconds());
    }
}
