package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.ShownTower;
import com.mycity.runwal.feed.domain.model.reaction.ReactionCounts;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A published post as the public sees it: "Verified resident", the tower only if the author chose to show it, the
 * photos and READY videos (short-lived links) and how many reacted with each emoji. Deliberately has no author and no internal ID.
 */
public record PublicPostView(String slug, String title, String body, String tower, Instant publishedAt,
                             List<EvidenceView> evidence, List<VideoView> videos, Map<String, Integer> reactions) {

    public static PublicPostView of(Post post, ReactionCounts reactions, EvidenceStorage storage) {
        return new PublicPostView(post.slug().value(), post.content().title(), post.content().body(),
                post.shownTower().map(ShownTower::label).orElse(null), post.publishedAt().orElse(null),
                post.evidence().stream().map(e -> EvidenceView.of(e, storage)).toList(),
                VideoView.watchable(post.videos(), storage), byName(reactions));
    }

    /** Emoji name -> count, only emoji someone chose. */
    public static Map<String, Integer> byName(ReactionCounts reactions) {
        Map<String, Integer> named = new TreeMap<>();
        reactions.byEmoji().forEach((emoji, count) -> named.put(emoji.name(), count));
        return Map.copyOf(named);
    }
}
