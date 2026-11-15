package com.mycity.runwal.feed.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReactionJpaRepository extends JpaRepository<ReactionJpaEntity, Long> {

    /** One statement, so two taps at once from the same visitor cannot both insert. */
    @Modifying
    @Query(value = """
            insert into feed.post_reaction (post_id, reactor_key, emoji, reacted_at)
            values (:postId, :reactorKey, :emoji, :reactedAt)
            on conflict (post_id, reactor_key) do update set emoji = excluded.emoji, reacted_at = excluded.reacted_at""",
            nativeQuery = true)
    void upsert(@Param("postId") long postId, @Param("reactorKey") String reactorKey, @Param("emoji") String emoji,
                @Param("reactedAt") Instant reactedAt);

    @Modifying
    @Query("delete from ReactionJpaEntity r where r.postId = :postId and r.reactorKey = :reactorKey")
    void deleteByReactor(@Param("postId") long postId, @Param("reactorKey") String reactorKey);

    /** Rows of (post id, emoji, count). */
    @Query("""
            select r.postId, r.emoji, count(r) from ReactionJpaEntity r
            where r.postId in :postIds
            group by r.postId, r.emoji""")
    List<Object[]> countByPostAndEmoji(@Param("postIds") Collection<Long> postIds);
}
