package com.mycity.runwal.workforce.application.service;

import com.mycity.runwal.workforce.application.port.SelfieStorage;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

/** Remembers which keys were "uploaded" (by the test) and which were deleted. */
final class FakeSelfieStorage implements SelfieStorage {

    final Map<String, Long> stored = new HashMap<>();

    void put(String key) {
        stored.put(key, 120_000L);
    }

    @Override
    public URI uploadUrl(String storageKey, String contentType, long sizeBytes, Duration validFor) {
        return URI.create("https://bucket.test/" + storageKey + "?put");
    }

    @Override
    public OptionalLong sizeOf(String storageKey) {
        Long size = stored.get(storageKey);
        return size == null ? OptionalLong.empty() : OptionalLong.of(size);
    }

    @Override
    public URI viewUrl(String storageKey, Duration validFor) {
        return URI.create("https://bucket.test/" + storageKey + "?get");
    }

    @Override
    public void delete(String storageKey) {
        stored.remove(storageKey);
    }
}
