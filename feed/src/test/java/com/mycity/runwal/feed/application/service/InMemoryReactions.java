package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.reaction.Emoji;
import com.mycity.runwal.feed.domain.model.reaction.Reaction;
import com.mycity.runwal.feed.domain.model.reaction.ReactionCounts;
import com.mycity.runwal.feed.domain.model.reaction.ReactionRepository;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class InMemoryReactions implements ReactionRepository {

    private final Map<String, Reaction> reactions = new LinkedHashMap<>();

    @Override
    public void save(Reaction reaction) {
        reactions.put(reaction.postId().value() + "/" + reaction.reactorKey(), reaction);
    }

    @Override
    public void remove(PostId post, String reactorKey) {
        reactions.remove(post.value() + "/" + reactorKey);
    }

    @Override
    public Map<PostId, ReactionCounts> countsFor(Collection<PostId> posts) {
        Map<PostId, Map<Emoji, Integer>> counts = new HashMap<>();
        reactions.values().stream().filter(r -> posts.contains(r.postId())).forEach(r ->
                counts.computeIfAbsent(r.postId(), id -> new EnumMap<>(Emoji.class)).merge(r.emoji(), 1, Integer::sum));
        Map<PostId, ReactionCounts> result = new HashMap<>();
        counts.forEach((post, byEmoji) -> result.put(post, new ReactionCounts(byEmoji)));
        return result;
    }
}
