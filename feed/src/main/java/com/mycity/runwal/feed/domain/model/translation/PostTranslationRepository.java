package com.mycity.runwal.feed.domain.model.translation;

import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import java.util.Optional;

@DomainRepository
public interface PostTranslationRepository {

    Optional<PostTranslation> find(PostId post, Language language);

    /** Stores the translation; if one for this post and language is already there, that one is kept. */
    void save(PostTranslation translation);

    /** Forgets the stored translation, if any, so a new one can be saved. */
    void remove(PostId post, Language language);
}
