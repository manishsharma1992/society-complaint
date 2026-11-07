package com.mycity.runwal.workforce.application.port;

import java.net.URI;
import java.time.Duration;
import java.util.OptionalLong;

/** The private bucket holding shift selfies. Clients reach objects only through signed links. */
public interface SelfieStorage {

    /** A link to upload exactly one object with this key, content type and size, with an HTTP PUT, until it expires. */
    URI uploadUrl(String storageKey, String contentType, long sizeBytes, Duration validFor);

    /** The size of the object stored under this key, or empty when there is none. */
    OptionalLong sizeOf(String storageKey);

    /** A link to view the object until it expires. */
    URI viewUrl(String storageKey, Duration validFor);

    /** Deletes the object; nothing happens when there is none. */
    void delete(String storageKey);
}
