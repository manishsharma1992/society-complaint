package com.mycity.runwal.feed.domain.model.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.feed.domain.event.PostHidden;
import com.mycity.runwal.feed.domain.event.PostPublished;
import com.mycity.runwal.feed.domain.event.PostRejected;
import com.mycity.runwal.feed.domain.event.PostRemoved;
import com.mycity.runwal.feed.domain.event.PostReported;
import com.mycity.runwal.feed.domain.event.PostSubmitted;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.exception.InvalidPostTransitionException;
import com.mycity.runwal.feed.domain.exception.PostingLimitReachedException;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostTest {

    private static final Instant NOW = Instant.parse("2026-11-01T06:00:00Z");
    private static final UserId AUTHOR = new UserId(10);
    private static final UserId MODERATOR = new UserId(1);

    private static Post submitted() {
        Post post = Post.submit(new PostId(7), new SocietyId(1), new PostSlug("abcdefgh2345"), AUTHOR,
                new PostContent("Gym promised in brochure", "Brochure page 4 shows a gym. Tower B2 has none."),
                null, NOW, 0);
        post.pullDomainEvents();
        return post;
    }

    private static Post published() {
        Post post = submitted();
        post.publish(MODERATOR, NOW);
        post.pullDomainEvents();
        return post;
    }

    private static PostReport report(String reporter) {
        return new PostReport(reporter, "Names a person", NOW.plusSeconds(60));
    }

    @Test
    void submittedPostWaitsForReviewAndItsEventDoesNotNameTheAuthor() {
        Post post = Post.submit(new PostId(7), new SocietyId(1), new PostSlug("abcdefgh2345"), AUTHOR,
                new PostContent(" Title ", " Facts "), null, NOW, 4);

        assertThat(post.status()).isEqualTo(PostStatus.PENDING_REVIEW);
        assertThat(post.content().title()).isEqualTo("Title");
        assertThat(post.isPublished()).isFalse();
        assertThat(post.publishedAt()).isEmpty();
        List<DomainEvent> events = post.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(PostSubmitted.class);
        assertThat(events.getFirst().toString()).doesNotContain("10");
        assertThat(post.moderationLog()).singleElement().satisfies(entry -> {
            assertThat(entry.from()).isNull();
            assertThat(entry.to()).isEqualTo(PostStatus.PENDING_REVIEW);
            assertThat(entry.moderator()).isNull();
        });
    }

    @Test
    void atMostFivePostsADay() {
        assertThatThrownBy(() -> Post.submit(new PostId(7), new SocietyId(1), new PostSlug("abcdefgh2345"), AUTHOR,
                new PostContent("T", "B"), null, NOW, 5))
                .isInstanceOf(PostingLimitReachedException.class)
                .hasMessageNotContaining("10");
    }

    @Test
    void moderatorPublishesAPendingPost() {
        Post post = submitted();

        post.publish(MODERATOR, NOW.plusSeconds(5));

        assertThat(post.isPublished()).isTrue();
        assertThat(post.publishedAt()).contains(NOW.plusSeconds(5));
        assertThat(post.pullDomainEvents()).singleElement().isInstanceOf(PostPublished.class);
        assertThat(post.moderationLog()).last().satisfies(entry -> {
            assertThat(entry.sequence()).isEqualTo(2);
            assertThat(entry.from()).isEqualTo(PostStatus.PENDING_REVIEW);
            assertThat(entry.moderator()).isEqualTo(MODERATOR);
        });
    }

    @Test
    void rejectingNeedsAReasonAndIsFinal() {
        Post post = submitted();
        assertThatThrownBy(() -> post.reject(MODERATOR, NOW, " ")).isInstanceOf(InvalidPostException.class);

        post.reject(MODERATOR, NOW, " Names a guard ");

        assertThat(post.status()).isEqualTo(PostStatus.REJECTED);
        assertThat(post.moderationLog()).last().extracting(ModerationSnapshot::reason).isEqualTo("Names a guard");
        assertThat(post.pullDomainEvents()).singleElement().isInstanceOf(PostRejected.class);
        assertThatThrownBy(() -> post.publish(MODERATOR, NOW)).isInstanceOf(InvalidPostTransitionException.class);
    }

    @Test
    void hiddenPostCanBeRestoredOrRemoved() {
        Post restored = published();
        restored.hide(MODERATOR, NOW.plusSeconds(10), "Checking a claim");
        restored.publish(MODERATOR, NOW.plusSeconds(20));
        assertThat(restored.isPublished()).isTrue();
        assertThat(restored.publishedAt()).contains(NOW);

        Post removed = published();
        removed.hide(MODERATOR, NOW, "Checking");
        removed.remove(MODERATOR, NOW, "Unverified allegation");
        assertThat(removed.status()).isEqualTo(PostStatus.REMOVED);
        assertThat(removed.pullDomainEvents()).hasSize(2).last().isInstanceOf(PostRemoved.class);
        assertThatThrownBy(() -> removed.publish(MODERATOR, NOW)).isInstanceOf(InvalidPostTransitionException.class);
    }

    @Test
    void invalidTransitionsAreRefused() {
        Post pending = submitted();
        assertThatThrownBy(() -> pending.hide(MODERATOR, NOW, "x")).isInstanceOf(InvalidPostTransitionException.class);
        assertThatThrownBy(() -> pending.remove(MODERATOR, NOW, "x")).isInstanceOf(InvalidPostTransitionException.class);

        Post published = published();
        assertThatThrownBy(() -> published.publish(MODERATOR, NOW)).isInstanceOf(InvalidPostTransitionException.class);
        assertThatThrownBy(() -> published.remove(MODERATOR, NOW, "x"))
                .isInstanceOf(InvalidPostTransitionException.class);
        assertThatThrownBy(() -> published.reject(MODERATOR, NOW, "x"))
                .isInstanceOf(InvalidPostTransitionException.class);
    }

    @Test
    void thirdReportHidesThePostUntilReviewed() {
        Post post = published();

        post.report(report("a"));
        post.report(report("b"));
        assertThat(post.isPublished()).isTrue();
        post.report(report("c"));

        assertThat(post.status()).isEqualTo(PostStatus.HIDDEN);
        assertThat(post.pullDomainEvents()).extracting(e -> e.getClass().getSimpleName())
                .containsExactly("PostReported", "PostReported", "PostReported", "PostHidden");
        assertThat(post.moderationLog()).last().satisfies(entry -> {
            assertThat(entry.moderator()).isNull();
            assertThat(entry.reason()).isEqualTo("3 reports");
        });

        post.publish(MODERATOR, NOW.plusSeconds(600));
        assertThat(post.openReports()).isEmpty();
        post.report(report("a"));
        assertThat(post.isPublished()).isTrue();
    }

    @Test
    void oneReportPerReporterAndOnlyOnPublishedPosts() {
        Post post = published();
        post.report(report("a"));
        assertThatThrownBy(() -> post.report(report("a"))).isInstanceOf(InvalidPostException.class);
        assertThat(post.pullDomainEvents()).singleElement().isInstanceOf(PostReported.class);

        Post pending = submitted();
        assertThatThrownBy(() -> pending.report(report("a"))).isInstanceOf(InvalidPostException.class);
    }

    @Test
    void hidingByAModeratorIsLoggedWithTheModerator() {
        Post post = published();
        post.hide(MODERATOR, NOW, "Face of a guard visible");
        assertThat(post.pullDomainEvents()).singleElement().isInstanceOfSatisfying(PostHidden.class,
                event -> assertThat(event.moderator()).isEqualTo(MODERATOR));
    }

    @Test
    void valueObjectsValidate() {
        assertThatThrownBy(() -> new PostSlug("ABC")).isInstanceOf(InvalidPostException.class);
        assertThatThrownBy(() -> new PostContent("", "body")).isInstanceOf(InvalidPostException.class);
        assertThatThrownBy(() -> new PostContent("t", "x".repeat(4001))).isInstanceOf(InvalidPostException.class);
        assertThatThrownBy(() -> new PostReport("k", " ", NOW)).isInstanceOf(InvalidPostException.class);
    }
}
