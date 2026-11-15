package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.application.port.Translator;
import com.mycity.runwal.feed.application.query.PostTranslationView;
import com.mycity.runwal.feed.application.query.TranslatePostQuery;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.translation.Language;
import com.mycity.runwal.feed.domain.model.translation.PostTranslation;
import com.mycity.runwal.feed.domain.model.translation.PostTranslationRepository;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import java.time.Clock;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anyone, even without logging in, can read a published post machine-translated into one of the app's languages.
 * The first request translates and stores it; everyone after gets the stored copy, so the paid service is used
 * at most once per post and language however often the button is pressed.
 */
@ApplicationService
public class PostTranslationService {

    private final PostRepository posts;
    private final PostTranslationRepository translations;
    private final Translator translator;
    private final Clock clock;

    public PostTranslationService(PostRepository posts, PostTranslationRepository translations, Translator translator,
                                  Clock clock) {
        this.posts = posts;
        this.translations = translations;
        this.translator = translator;
        this.clock = clock;
    }

    /** Empty unless the post is published. */
    // shortcut: the first translation calls the service inside the transaction (holds a DB connection ~1 s);
    // fine while only the first reader per post and language waits, split it if that grows
    @Transactional
    public Optional<PostTranslationView> translate(TranslatePostQuery query) {
        Language language = Language.ofCode(query.language());
        return findPublished(query.slug())
                .map(post -> translations.find(post.id(), language).orElseGet(() -> {
                    PostTranslation made = PostTranslation.of(post, language,
                            translator.translate(post.content().title(), language),
                            translator.translate(post.content().body(), language), clock.instant());
                    translations.save(made);
                    return made;
                }))
                .map(PostTranslationView::of);
    }

    private Optional<Post> findPublished(String slug) {
        PostSlug postSlug;
        try {
            postSlug = new PostSlug(slug);
        } catch (InvalidPostException malformed) {
            return Optional.empty();
        }
        return posts.findBySlug(postSlug).filter(Post::isPublished);
    }
}
