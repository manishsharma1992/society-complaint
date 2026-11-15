package com.mycity.runwal.feed.domain.model.reaction;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import java.util.EnumMap;
import java.util.Map;

/** How many reactions of each emoji a post has; emoji nobody chose are left out. */
@ValueObject
public record ReactionCounts(Map<Emoji, Integer> byEmoji) {

    public static final ReactionCounts NONE = new ReactionCounts(Map.of());

    public ReactionCounts {
        EnumMap<Emoji, Integer> positive = new EnumMap<>(Emoji.class);
        byEmoji.forEach((emoji, count) -> {
            if (count > 0) {
                positive.put(emoji, count);
            }
        });
        byEmoji = Map.copyOf(positive);
    }
}
