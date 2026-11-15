package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.application.command.RedoTranslationCommand;
import com.mycity.runwal.feed.application.port.CurrentActor;
import com.mycity.runwal.feed.application.port.Translator;
import com.mycity.runwal.feed.application.query.PostTranslationView;
import com.mycity.runwal.feed.application.query.TranslatePostQuery;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.model.actor.Actor;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.translation.Language;
import com.mycity.runwal.feed.domain.model.translation.PostTranslation;
import com.mycity.runwal.feed.domain.model.translation.PostTranslationRepository;
import com.mycity.runwal.feed.domain.service.FeedAccessPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anyone, even without logging in, can read a published post machine-translated into one of the app's languages.
 * The first request translates and stores it; everyone after gets the stored copy, so the paid service is used
 * at most once per post and language however often the button is pressed.
 */
@ApplicationService
public class PostTranslationService {

    private static final Logger log = LoggerFactory.getLogger(PostTranslationService.class);

    private final PostRepository posts;
    private final PostTranslationRepository translations;
    private final Translator translator;
    private final FeedAccessPolicy accessPolicy;
    private final CurrentActor currentActor;
    private final Clock clock;

    public PostTranslationService(PostRepository posts, PostTranslationRepository translations, Translator translator,
                                  FeedAccessPolicy accessPolicy, CurrentActor currentActor, Clock clock) {
        this.posts = posts;
        this.translations = translations;
        this.translator = translator;
        this.accessPolicy = accessPolicy;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    /**
     * The society admin has the translation made again, e.g. after a bad one. The stored one is replaced only once
     * the new one has arrived. Empty unless the society has this post published.
     */
    @Transactional
    public Optional<PostTranslationView> redo(RedoTranslationCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        Actor actor = currentActor.actor();
        if (!accessPolicy.mayModerate(actor, societyId)) {
            throw new ActionNotAllowedException("Only the society admin can redo a translation");
        }
        Language language = Language.ofCode(command.language());
        return findPublished(command.slug())
                .filter(post -> post.societyId().equals(societyId))
                .map(post -> {
                    PostTranslation made = translate(post, language);
                    translations.remove(post.id(), language);
                    translations.save(made);
                    log.info("Translation of post {} into {} made again by user {}", post.slug().value(),
                            language.code(), actor.userId().value());
                    return PostTranslationView.of(made);
                });
    }

    private PostTranslation translate(Post post, Language language) {
        return PostTranslation.of(post, language, translator.translate(post.content().title(), language),
                translator.translate(post.content().body(), language), clock.instant());
    }

    /** Empty unless the post is published. */
    // shortcut: the first translation calls the service inside the transaction (holds a DB connection ~1 s);
    // fine while only the first reader per post and language waits, split it if that grows
    @Transactional
    public Optional<PostTranslationView> translate(TranslatePostQuery query) {
        Language language = Language.ofCode(query.language());
        return findPublished(query.slug())
                .map(post -> translations.find(post.id(), language).orElseGet(() -> {
                    PostTranslation made = translate(post, language);
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
