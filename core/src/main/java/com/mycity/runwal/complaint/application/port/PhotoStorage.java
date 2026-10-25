package com.mycity.runwal.complaint.application.port;

import java.net.URI;
import java.time.Duration;
import java.util.OptionalLong;

/** Object storage holding complaint photos. The bucket is private: photos are reached only through signed links. */
public interface PhotoStorage {

    /** A link to upload exactly one object with this key and content type, with an HTTP PUT, until it expires. */
    URI uploadUrl(String storageKey, String contentType, long sizeBytes, Duration validFor);

    /** The size of the object stored under this key, or empty when nothing has been uploaded there. */
    OptionalLong sizeOf(String storageKey);

    /** A link to view the object until it expires. */
    URI viewUrl(String storageKey, Duration validFor);
}
