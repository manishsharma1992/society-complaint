package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.translation.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Mapped for reading only: writes go through the insert in {@link PostTranslationJpaRepository}. */
@Entity
@Table(schema = "feed", name = "post_translation")
class PostTranslationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "post_id", nullable = false)
    long postId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    Language language;

    @Column(nullable = false, columnDefinition = "text")
    String title;

    @Column(nullable = false, columnDefinition = "text")
    String body;

    @Column(name = "translated_at", nullable = false)
    Instant translatedAt;

    protected PostTranslationJpaEntity() {
    }
}
