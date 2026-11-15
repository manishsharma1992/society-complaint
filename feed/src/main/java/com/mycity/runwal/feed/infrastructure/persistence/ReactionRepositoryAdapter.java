package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.reaction.Emoji;
import com.mycity.runwal.feed.domain.model.reaction.Reaction;
import com.mycity.runwal.feed.domain.model.reaction.ReactionCounts;
import com.mycity.runwal.feed.domain.model.reaction.ReactionRepository;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class ReactionRepositoryAdapter implements ReactionRepository {

    private final ReactionJpaRepository jpaRepository;

    ReactionRepositoryAdapter(ReactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Reaction reaction) {
        jpaRepository.upsert(reaction.postId().value(), reaction.reactorKey(), reaction.emoji().name(),
                reaction.reactedAt());
    }

    @Override
    public void remove(PostId post, String reactorKey) {
        jpaRepository.deleteByReactor(post.value(), reactorKey);
    }

    @Override
    public Map<PostId, ReactionCounts> countsFor(Collection<PostId> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        Map<PostId, Map<Emoji, Integer>> counts = new HashMap<>();
        for (Object[] row : jpaRepository.countByPostAndEmoji(posts.stream().map(PostId::value).toList())) {
            counts.computeIfAbsent(new PostId((Long) row[0]), id -> new EnumMap<>(Emoji.class))
                    .put((Emoji) row[1], Math.toIntExact((Long) row[2]));
        }
        Map<PostId, ReactionCounts> result = new HashMap<>();
        counts.forEach((post, byEmoji) -> result.put(post, new ReactionCounts(byEmoji)));
        return result;
    }
}
