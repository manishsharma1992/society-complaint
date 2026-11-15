package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.translation.Language;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostTranslationJpaRepository extends JpaRepository<PostTranslationJpaEntity, Long> {

    Optional<PostTranslationJpaEntity> findByPostIdAndLanguage(long postId, Language language);

    @Modifying(flushAutomatically = true)
    @Query("delete from PostTranslationJpaEntity t where t.postId = :postId and t.language = :language")
    void deleteByPost(@Param("postId") long postId, @Param("language") Language language);

    /** One statement, so two readers asking at once cannot both insert. */
    @Modifying
    @Query(value = """
            insert into feed.post_translation (post_id, language, title, body, translated_at)
            values (:postId, :language, :title, :body, :translatedAt)
            on conflict (post_id, language) do nothing""",
            nativeQuery = true)
    void insertIfAbsent(@Param("postId") long postId, @Param("language") String language,
                        @Param("title") String title, @Param("body") String body,
                        @Param("translatedAt") Instant translatedAt);
}
