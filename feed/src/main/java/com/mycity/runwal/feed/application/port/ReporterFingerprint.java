package com.mycity.runwal.feed.application.port;

import com.mycity.runwal.feed.domain.model.post.PostSlug;

/**
 * Tells reporters and reactors of a post apart without storing who they are: a keyed hash of their network address
 * and the post, so the same visitor gives a different key on every post and the key cannot be turned back into an
 * address.
 */
public interface ReporterFingerprint {

    String keyFor(String reporterAddress, PostSlug post);
}
