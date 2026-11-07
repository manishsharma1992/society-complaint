package com.mycity.runwal.feed.application.port;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.function.Function;

/** The private bucket holding feed photos. Clients reach objects only through signed links. */
public interface EvidenceStorage {

    /** A link to upload exactly one object with this key, content type and size, with an HTTP PUT, until it expires. */
    URI uploadUrl(String storageKey, String contentType, long sizeBytes, Duration validFor);

    /** Copies an object inside the bucket; false when nothing was uploaded under {@code fromKey}. */
    boolean copy(String fromKey, String toKey);

    /** The size of the object stored under this key, or empty when there is none. */
    OptionalLong sizeOf(String storageKey);

    /** Streams the object to {@code reader} and closes the stream afterwards. */
    <T> T read(String storageKey, Function<InputStream, T> reader);

    /** Deletes the object; nothing happens when there is none. */
    void delete(String storageKey);

    /** A link to view the object until it expires. */
    URI viewUrl(String storageKey, Duration validFor);
}
