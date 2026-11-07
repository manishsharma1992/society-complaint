package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.post.PostStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(schema = "feed", name = "post_moderation")
class PostModerationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "sequence_no", nullable = false)
    int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    PostStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    PostStatus toStatus;

    @Column(name = "moderator_id")
    Long moderatorId;

    @Column(name = "moderated_at", nullable = false)
    Instant moderatedAt;

    @Column(length = 500)
    String reason;

    protected PostModerationJpaEntity() {
    }
}
