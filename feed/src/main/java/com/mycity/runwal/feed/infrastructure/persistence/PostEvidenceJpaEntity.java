package com.mycity.runwal.feed.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(schema = "feed", name = "post_evidence")
class PostEvidenceJpaEntity {

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

    @Column(name = "width_px", nullable = false)
    int widthPx;

    @Column(name = "height_px", nullable = false)
    int heightPx;

    @Column(name = "uploaded_at", nullable = false)
    Instant uploadedAt;

    protected PostEvidenceJpaEntity() {
    }
}
