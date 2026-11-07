package com.mycity.runwal.feed.application.port;

import com.mycity.runwal.feed.domain.model.post.ImageFacts;
import java.io.InputStream;

/**
 * Reads an uploaded file's headers without decoding the picture: its real type from the first bytes, its size in
 * pixels, and which metadata is still embedded.
 */
public interface ImageInspector {

    /** Never throws for bad input: a file that cannot be read is {@link ImageFacts#notAnImage()}. */
    ImageFacts inspect(InputStream file);
}
