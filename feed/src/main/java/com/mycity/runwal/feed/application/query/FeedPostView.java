package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.ShownTower;
import com.mycity.runwal.feed.domain.model.reaction.ReactionCounts;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A published post in a signed-in resident's feed: "Verified resident" of {@code society}, the tower only if the
 * author chose to show it, the photos (short-lived links) and how many reacted with each emoji.
 * Deliberately has no author and no internal ID.
 */
public record FeedPostView(String slug, String title, String body, String society, String tower,
                           Instant publishedAt, List<EvidenceView> evidence, Map<String, Integer> reactions) {

    public static FeedPostView of(Post post, String society, ReactionCounts reactions, EvidenceStorage storage) {
        return new FeedPostView(post.slug().value(), post.content().title(), post.content().body(), society,
                post.shownTower().map(ShownTower::label).orElse(null), post.publishedAt().orElse(null),
                post.evidence().stream().map(e -> EvidenceView.of(e, storage)).toList(),
                PublicPostView.byName(reactions));
    }
}
