package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.post.VideoStatus;
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
@Table(schema = "feed", name = "post_video")
class PostVideoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    int position;

    @Column(name = "storage_key", nullable = false, length = 300)
    String storageKey;

    @Column(name = "content_type", nullable = false, length = 20)
    String contentType;

    @Column(name = "size_bytes", nullable = false)
    long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    Instant uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    VideoStatus status;

    @Column(name = "width_px")
    Integer widthPx;

    @Column(name = "height_px")
    Integer heightPx;

    @Column(name = "duration_ms")
    Long durationMs;

    @Column(name = "failure_reason", length = 300)
    String failureReason;

    protected PostVideoJpaEntity() {
    }
}
