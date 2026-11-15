package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.reaction.Emoji;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Mapped for reading only: writes go through the upsert in {@link ReactionJpaRepository}. */
@Entity
@Table(schema = "feed", name = "post_reaction")
class ReactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "post_id", nullable = false)
    long postId;

    @Column(name = "reactor_key", nullable = false, length = 100)
    String reactorKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    Emoji emoji;

    @Column(name = "reacted_at", nullable = false)
    Instant reactedAt;

    protected ReactionJpaEntity() {
    }
}
