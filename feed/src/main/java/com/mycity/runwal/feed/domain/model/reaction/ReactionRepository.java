package com.mycity.runwal.feed.domain.model.reaction;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import java.util.Collection;
import java.util.Map;

@DomainRepository
public interface ReactionRepository {

    /** Stores the reaction, replacing the same reactor's earlier one on that post. */
    void save(Reaction reaction);

    /** Takes back the reactor's reaction on the post, if there is one. */
    void remove(PostId post, String reactorKey);

    /** The counts of each of these posts; a post without reactions is left out. */
    Map<PostId, ReactionCounts> countsFor(Collection<PostId> posts);
}
