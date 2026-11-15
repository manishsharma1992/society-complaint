package com.mycity.runwal.feed.domain.model.reaction;

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
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReactionTest {

    private static final Instant NOW = Instant.parse("2026-11-15T06:00:00Z");

    private static Post post() {
        return Post.submit(new PostId(7), new SocietyId(1), new PostSlug("abcdefgh2345"), new UserId(10),
                new PostContent("No gym", "Brochure p.4 promised a gym."), null, NOW, 0);
    }

    @Test
    void onlyPublishedPostsTakeReactions() {
        Post post = post();
        assertThatThrownBy(() -> Reaction.to(post, "visitor", Emoji.HEART, NOW))
                .isInstanceOf(InvalidPostException.class);

        post.publish(new UserId(1), NOW);
        Reaction reaction = Reaction.to(post, "visitor", Emoji.HEART, NOW);
        assertThat(reaction.postId()).isEqualTo(new PostId(7));
        assertThat(reaction.emoji()).isEqualTo(Emoji.HEART);
    }

    @Test
    void aReactionNeedsAReactorKey() {
        Post post = post();
        post.publish(new UserId(1), NOW);
        assertThatThrownBy(() -> Reaction.to(post, " ", Emoji.HEART, NOW)).isInstanceOf(InvalidPostException.class);
        assertThatThrownBy(() -> Reaction.to(post, "k".repeat(101), Emoji.HEART, NOW))
                .isInstanceOf(InvalidPostException.class);
    }

    @Test
    void countsLeaveOutEmojiNobodyChose() {
        assertThat(new ReactionCounts(Map.of(Emoji.HEART, 2, Emoji.SAD, 0)).byEmoji())
                .isEqualTo(Map.of(Emoji.HEART, 2));
    }
}
