package com.mycity.runwal.feed.domain.model.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostContent;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PostTranslationTest {

    private static final Instant NOW = Instant.parse("2026-11-15T06:00:00Z");

    @Test
    void onlyPublishedPostsAreTranslated() {
        Post post = Post.submit(new PostId(7), new SocietyId(1), new PostSlug("abcdefgh2345"), new UserId(10),
                new PostContent("No gym", "Brochure p.4 promised a gym."), null, NOW, 0);
        assertThatThrownBy(() -> PostTranslation.of(post, Language.MR, "जिम नाही", "माहितीपत्रक", NOW))
                .isInstanceOf(InvalidPostException.class);

        post.publish(new UserId(1), NOW);
        PostTranslation translation = PostTranslation.of(post, Language.MR, "जिम नाही", "माहितीपत्रक", NOW);
        assertThat(translation.postId()).isEqualTo(new PostId(7));
        assertThat(translation.language()).isEqualTo(Language.MR);
    }

    @Test
    void languagesAreTheWebAppsCodes() {
        assertThat(Language.ofCode("hi")).isEqualTo(Language.HI);
        assertThat(Language.MR.code()).isEqualTo("mr");
        assertThatThrownBy(() -> Language.ofCode("fr")).isInstanceOf(InvalidPostException.class);
        assertThatThrownBy(() -> Language.ofCode("HI")).isInstanceOf(InvalidPostException.class);
    }
}
