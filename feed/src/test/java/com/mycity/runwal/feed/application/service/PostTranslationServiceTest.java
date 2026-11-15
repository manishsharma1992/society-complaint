package com.mycity.runwal.feed.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.feed.application.query.PostTranslationView;
import com.mycity.runwal.feed.application.query.TranslatePostQuery;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
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
    };
    private int calls;
    private final PostTranslationService service = new PostTranslationService(posts, translations,
            (text, target) -> {
                calls++;
                return "[" + target.code() + "] " + text;
            }, Clock.fixed(NOW, ZoneOffset.UTC));

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
    void anUnknownLanguageIsRefused() {
        post("abcdefgh2345", true);
        assertThatThrownBy(() -> service.translate(new TranslatePostQuery("abcdefgh2345", "fr")))
                .isInstanceOf(InvalidPostException.class);
        assertThat(calls).isZero();
    }
}
