package com.mycity.runwal.feed.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.feed.application.query.CommunityFeedQuery;
import com.mycity.runwal.feed.application.query.FeedPostQuery;
import com.mycity.runwal.feed.application.query.FeedPostView;
import com.mycity.runwal.feed.application.query.NeighbourView;
import com.mycity.runwal.feed.domain.model.actor.Actor;
import com.mycity.runwal.feed.domain.model.actor.ActorRole;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostContent;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Runwal MyCity (1) and Palava (2) are neighbours; Thane (3) is too far. Posts: Runwal published (older), Palava
 * published (newer), Thane published, and a Runwal post still waiting for review.
 */
class CommunityFeedServiceTest {

    private static final SocietyId RUNWAL = new SocietyId(1);
    private static final SocietyId PALAVA = new SocietyId(2);
    private static final SocietyId THANE = new SocietyId(3);
    private static final Instant NOW = Instant.parse("2026-11-07T06:00:00Z");

    private final InMemoryPosts posts = new InMemoryPosts();
    private Actor current = member(10, RUNWAL);
    private final CommunityFeedService feed = new CommunityFeedService(posts,
            society -> society.equals(RUNWAL)
                    ? List.of(new NeighbourView(RUNWAL, "Runwal MyCity"), new NeighbourView(PALAVA, "Palava"))
                    : List.of(),
            () -> current, new FakeEvidenceStorage());

    private static Actor member(long id, SocietyId society) {
        return new Actor(new UserId(id), true, Set.of(new ActorRole(ActorRole.Part.RESIDENT, society)));
    }

    private String store(SocietyId society, String slug, Instant publishedAt) {
        Post post = Post.submit(posts.nextId(), society, new PostSlug(slug), new UserId(50),
                new PostContent("Title " + slug, "Facts"), null, NOW.minus(Duration.ofDays(30)), 0);
        if (publishedAt != null) {
            post.publish(new UserId(1), publishedAt);
        }
        post.pullDomainEvents();
        posts.save(post);
        return slug;
    }

    CommunityFeedServiceTest() {
        store(RUNWAL, "runwal000001", NOW.minus(Duration.ofDays(2)));
        store(PALAVA, "palava000001", NOW.minus(Duration.ofDays(1)));
        store(THANE, "thane0000001", NOW.minus(Duration.ofHours(1)));
        store(RUNWAL, "pending00001", null);
    }

    @Test
    void membersSeePublishedPostsOfTheirSocietyAndNearbyOnesNewestFirstLabelledBySociety() {
        List<FeedPostView> shown = feed.feed(new CommunityFeedQuery(1, 0, 20));

        assertThat(shown).extracting(FeedPostView::slug).containsExactly("palava000001", "runwal000001");
        assertThat(shown).extracting(FeedPostView::society).containsExactly("Palava", "Runwal MyCity");
        assertThat(feed.feed(new CommunityFeedQuery(1, 1, 1))).extracting(FeedPostView::slug)
                .containsExactly("runwal000001");
    }

    @Test
    void aSinglePostOpensOnlyWhenPublishedAndFromTheFeedsSocieties() {
        assertThat(feed.post(new FeedPostQuery(1, "palava000001"))).hasValueSatisfying(
                view -> assertThat(view.society()).isEqualTo("Palava"));
        assertThat(feed.post(new FeedPostQuery(1, "thane0000001"))).isEmpty();
        assertThat(feed.post(new FeedPostQuery(1, "pending00001"))).isEmpty();
        assertThat(feed.post(new FeedPostQuery(1, "not a slug"))).isEmpty();
    }

    @Test
    void onlyApprovedMembersOfTheSocietySeeItsFeed() {
        current = member(11, PALAVA);
        assertThatThrownBy(() -> feed.feed(new CommunityFeedQuery(1, 0, 20)))
                .isInstanceOf(ActionNotAllowedException.class);
        current = new Actor(new UserId(12), false, member(12, RUNWAL).roles());
        assertThatThrownBy(() -> feed.feed(new CommunityFeedQuery(1, 0, 20)))
                .isInstanceOf(ActionNotAllowedException.class);
        current = new Actor(new UserId(13), true, Set.of(new ActorRole(ActorRole.Part.STAFF, RUNWAL)));
        assertThat(feed.feed(new CommunityFeedQuery(1, 0, 20))).hasSize(2);
    }
}
