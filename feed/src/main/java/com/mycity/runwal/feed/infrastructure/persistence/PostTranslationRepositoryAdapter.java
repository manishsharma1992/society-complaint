package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.translation.Language;
import com.mycity.runwal.feed.domain.model.translation.PostTranslation;
import com.mycity.runwal.feed.domain.model.translation.PostTranslationRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class PostTranslationRepositoryAdapter implements PostTranslationRepository {

    private final PostTranslationJpaRepository jpaRepository;

    PostTranslationRepositoryAdapter(PostTranslationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PostTranslation> find(PostId post, Language language) {
        return jpaRepository.findByPostIdAndLanguage(post.value(), language)
                .map(row -> PostTranslation.reconstitute(new PostId(row.postId), row.language, row.title, row.body,
                        row.translatedAt));
    }

    @Override
    public void save(PostTranslation translation) {
        jpaRepository.insertIfAbsent(translation.postId().value(), translation.language().name(),
                translation.title(), translation.body(), translation.translatedAt());
    }

    @Override
    public void remove(PostId post, Language language) {
        jpaRepository.deleteByPost(post.value(), language);
    }
}
