package com.mycity.runwal.feed.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(schema = "feed", name = "post_report")
class PostReportJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "reporter_key", nullable = false, length = 100)
    String reporterKey;

    @Column(nullable = false, length = 500)
    String reason;

    @Column(name = "reported_at", nullable = false)
    Instant reportedAt;

    protected PostReportJpaEntity() {
    }
}
