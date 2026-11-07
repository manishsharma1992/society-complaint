package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.ShownTower;
import java.time.Instant;
import java.util.List;

/**
 * A published post in a signed-in resident's feed: "Verified resident" of {@code society}, the tower only if the
 * author chose to show it, and the photos (short-lived links). Deliberately has no author and no internal ID.
 */
public record FeedPostView(String slug, String title, String body, String society, String tower,
                           Instant publishedAt, List<EvidenceView> evidence) {

    public static FeedPostView of(Post post, String society, EvidenceStorage storage) {
        return new FeedPostView(post.slug().value(), post.content().title(), post.content().body(), society,
                post.shownTower().map(ShownTower::label).orElse(null), post.publishedAt().orElse(null),
                post.evidence().stream().map(e -> EvidenceView.of(e, storage)).toList());
    }
}
