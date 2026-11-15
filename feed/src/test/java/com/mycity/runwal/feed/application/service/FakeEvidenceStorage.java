package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.application.port.EvidenceStorage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Function;

/** A bucket in memory: tests "upload" with {@link #put}. */
final class FakeEvidenceStorage implements EvidenceStorage {

    final Map<String, byte[]> objects = new HashMap<>();

    void put(String key, byte[] content) {
        objects.put(key, content);
    }

    @Override
    public URI uploadUrl(String storageKey, String contentType, long sizeBytes, Duration validFor) {
        return URI.create("https://bucket.test/" + storageKey + "?put");
    }

    @Override
    public boolean copy(String fromKey, String toKey) {
        byte[] content = objects.get(fromKey);
        if (content == null) {
            return false;
        }
        objects.put(toKey, content);
        return true;
    }

    @Override
    public OptionalLong sizeOf(String storageKey) {
        byte[] content = objects.get(storageKey);
        return content == null ? OptionalLong.empty() : OptionalLong.of(content.length);
    }

    @Override
    public <T> T read(String storageKey, Function<InputStream, T> reader) {
        return reader.apply(new ByteArrayInputStream(objects.get(storageKey)));
    }

    @Override
    public void put(String storageKey, Path file, String contentType) {
        try {
            objects.put(storageKey, Files.readAllBytes(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(String storageKey) {
        objects.remove(storageKey);
    }

    @Override
    public URI viewUrl(String storageKey, Duration validFor) {
        return URI.create("https://bucket.test/" + storageKey + "?get");
    }
}
