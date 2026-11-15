package com.mycity.runwal.feed.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.feed.application.command.RedoTranslationCommand;
import com.mycity.runwal.feed.application.query.PostTranslationView;
import com.mycity.runwal.feed.application.query.TranslatePostQuery;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.model.actor.Actor;
import com.mycity.runwal.feed.domain.model.actor.ActorRole;
import com.mycity.runwal.feed.domain.service.FeedAccessPolicy;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostContent;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.translation.Language;
import com.mycity.runwal.feed.domain.model.translation.PostTranslation;
import com.mycity.runwal.feed.domain.model.translation.PostTranslationRepository;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PostTranslationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T06:00:00Z");

    private final InMemoryPosts posts = new InMemoryPosts();
    private final Map<String, PostTranslation> stored = new HashMap<>();
    private final PostTranslationRepository translations = new PostTranslationRepository() {
        @Override
        public Optional<PostTranslation> find(PostId post, Language language) {
            return Optional.ofNullable(stored.get(post.value() + "/" + language));
        }

        @Override
        public void save(PostTranslation translation) {
            stored.putIfAbsent(translation.postId().value() + "/" + translation.language(), translation);
        }

        @Override
        public void remove(PostId post, Language language) {
            stored.remove(post.value() + "/" + language);
        }
    };
    private int calls;
    private String prefix = "";
    private boolean serviceDown;
    private Actor current = actor(1, ActorRole.Part.SOCIETY_ADMIN);
    private final PostTranslationService service = new PostTranslationService(posts, translations,
            (text, target) -> {
                if (serviceDown) {
                    throw new TranslationUnavailableException("down", null);
                }
                calls++;
                return prefix + "[" + target.code() + "] " + text;
            }, new FeedAccessPolicy(), () -> current, Clock.fixed(NOW, ZoneOffset.UTC));

    private static Actor actor(long id, ActorRole.Part part) {
        return new Actor(new UserId(id), true, Set.of(new ActorRole(part, new SocietyId(1))));
    }

    private Post post(String slug, boolean published) {
        Post post = Post.submit(posts.nextId(), new SocietyId(1), new PostSlug(slug), new UserId(10),
                new PostContent("No gym", "Brochure p.4 promised a gym."), null, NOW, 0);
        if (published) {
            post.publish(new UserId(1), NOW);
        }
        posts.save(post);
        return post;
    }

    @Test
    void translatesOnceAndServesTheStoredCopyAfterwards() {
        post("abcdefgh2345", true);

        PostTranslationView first = service.translate(new TranslatePostQuery("abcdefgh2345", "mr")).orElseThrow();
        assertThat(first).isEqualTo(new PostTranslationView("mr", "[mr] No gym", "[mr] Brochure p.4 promised a gym."));
        assertThat(calls).isEqualTo(2);

        assertThat(service.translate(new TranslatePostQuery("abcdefgh2345", "mr"))).contains(first);
        assertThat(calls).isEqualTo(2);

        service.translate(new TranslatePostQuery("abcdefgh2345", "hi"));
        assertThat(calls).isEqualTo(4);
    }

    @Test
    void unpublishedOrUnknownPostsAreNotTranslated() {
        post("pendingpost2", false);

        assertThat(service.translate(new TranslatePostQuery("pendingpost2", "mr"))).isEmpty();
        assertThat(service.translate(new TranslatePostQuery("zzzzzzzzzzzz", "mr"))).isEmpty();
        assertThat(service.translate(new TranslatePostQuery("not a slug", "mr"))).isEmpty();
        assertThat(calls).isZero();
    }

    @Test
    void theSocietyAdminHasATranslationMadeAgain() {
        post("abcdefgh2345", true);
        service.translate(new TranslatePostQuery("abcdefgh2345", "mr"));

        prefix = "better ";
        PostTranslationView redone = service.redo(new RedoTranslationCommand(1, "abcdefgh2345", "mr")).orElseThrow();

        assertThat(redone.title()).isEqualTo("better [mr] No gym");
        assertThat(service.translate(new TranslatePostQuery("abcdefgh2345", "mr"))).contains(redone);
    }

    @Test
    void whenTheServiceFailsTheStoredTranslationStays() {
        post("abcdefgh2345", true);
        PostTranslationView first = service.translate(new TranslatePostQuery("abcdefgh2345", "mr")).orElseThrow();

        serviceDown = true;
        assertThatThrownBy(() -> service.redo(new RedoTranslationCommand(1, "abcdefgh2345", "mr")))
                .isInstanceOf(TranslationUnavailableException.class);
        serviceDown = false;

        assertThat(service.translate(new TranslatePostQuery("abcdefgh2345", "mr"))).contains(first);
    }

    @Test
    void onlyTheSocietyAdminRedoesAndOnlyTheirSocietysPosts() {
        post("abcdefgh2345", true);
        current = actor(10, ActorRole.Part.RESIDENT);
        assertThatThrownBy(() -> service.redo(new RedoTranslationCommand(1, "abcdefgh2345", "mr")))
                .isInstanceOf(ActionNotAllowedException.class);

        current = new Actor(new UserId(2), true, Set.of(new ActorRole(ActorRole.Part.SOCIETY_ADMIN, new SocietyId(2))));
        assertThat(service.redo(new RedoTranslationCommand(2, "abcdefgh2345", "mr"))).isEmpty();
        assertThat(calls).isZero();
    }

    @Test
    void anUnknownLanguageIsRefused() {
        post("abcdefgh2345", true);
        assertThatThrownBy(() -> service.translate(new TranslatePostQuery("abcdefgh2345", "fr")))
                .isInstanceOf(InvalidPostException.class);
        assertThat(calls).isZero();
    }
}
