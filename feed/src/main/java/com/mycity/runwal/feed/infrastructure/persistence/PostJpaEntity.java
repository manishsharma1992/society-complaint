package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.post.PostStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(schema = "feed", name = "post")
class PostJpaEntity {

    /** Assigned from the post_id_seq sequence by {@link PostRepositoryAdapter#nextId()}. */
    @Id
    Long id;

    @Column(name = "society_id", nullable = false)
    long societyId;

    @Column(nullable = false, length = 12)
    String slug;

    @Column(name = "author_id", nullable = false)
    long authorId;

    @Column(nullable = false, length = 150)
    String title;

    @Column(nullable = false, length = 4000)
    String body;

    @Column(name = "tower_id")
    Long towerId;

    @Column(name = "tower_label", length = 120)
    String towerLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    PostStatus status;

    @Column(name = "submitted_at", nullable = false)
    Instant submittedAt;

    @Column(name = "published_at")
    Instant publishedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "post_id", nullable = false)
    @OrderBy("sequenceNo")
    @BatchSize(size = 50)
    List<PostModerationJpaEntity> moderation = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "post_id", nullable = false)
    @OrderBy("reportedAt")
    @BatchSize(size = 50)
    List<PostReportJpaEntity> reports = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "post_id", nullable = false)
    @OrderBy("position")
    @BatchSize(size = 50)
    List<PostEvidenceJpaEntity> evidence = new ArrayList<>();

    protected PostJpaEntity() {
    }

    PostJpaEntity(long id) {
        this.id = id;
    }
}
