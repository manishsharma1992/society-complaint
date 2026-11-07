package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.ShownTower;
import java.time.Instant;

/**
 * A published post as the public sees it: "Verified resident", the tower only if the author chose to show it.
 * Deliberately has no author and no internal ID.
 */
public record PublicPostView(String slug, String title, String body, String tower, Instant publishedAt) {

    public static PublicPostView of(Post post) {
        return new PublicPostView(post.slug().value(), post.content().title(), post.content().body(),
                post.shownTower().map(ShownTower::label).orElse(null), post.publishedAt().orElse(null));
    }
}
