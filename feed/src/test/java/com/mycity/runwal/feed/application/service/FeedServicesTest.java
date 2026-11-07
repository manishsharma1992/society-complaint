package com.mycity.runwal.feed.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.feed.application.command.ModeratePostCommand;
import com.mycity.runwal.feed.application.command.ModeratePostCommand.Decision;
import com.mycity.runwal.feed.application.command.ReportPostCommand;
import com.mycity.runwal.feed.application.command.SubmitPostCommand;
import com.mycity.runwal.feed.application.port.CurrentActor;
import com.mycity.runwal.feed.application.port.ResidentTowers;
import com.mycity.runwal.feed.application.query.GetPublicPostQuery;
import com.mycity.runwal.feed.application.query.ListPublicPostsQuery;
import com.mycity.runwal.feed.application.query.ModerationQueueQuery;
import com.mycity.runwal.feed.application.query.MyPostsQuery;
import com.mycity.runwal.feed.application.query.PostView;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.exception.PostingLimitReachedException;
import com.mycity.runwal.feed.domain.model.actor.Actor;
import com.mycity.runwal.feed.domain.model.actor.ActorRole;
import com.mycity.runwal.feed.domain.model.post.ShownTower;
import com.mycity.runwal.feed.domain.service.FeedAccessPolicy;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FeedServicesTest {

    private static final SocietyId SOCIETY = new SocietyId(1);
    private static final Actor RESIDENT = actor(10, true, ActorRole.Part.RESIDENT);
    private static final Actor ADMIN = actor(1, true, ActorRole.Part.SOCIETY_ADMIN);

    private final InMemoryPosts posts = new InMemoryPosts();
    private final List<DomainEvent> published = new ArrayList<>();
    private Actor current = RESIDENT;
    private final CurrentActor currentActor = () -> current;
    /** Resident 10 lives in tower 7, "B2 Radiant". */
    private final ResidentTowers towers = (society, user, tower) ->
            user.equals(new UserId(10)) && tower.equals(new TowerId(7))
                    ? Optional.of(new ShownTower(tower, "B2 Radiant"))
                    : Optional.empty();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T06:00:00Z"), ZoneOffset.UTC);

    private final FakeEvidenceStorage storage = new FakeEvidenceStorage();
    private final PostingService posting = new PostingService(posts, new FeedAccessPolicy(), currentActor, towers,
            published::addAll, storage, clock);
    private final ModerationService moderation = new ModerationService(posts, new FeedAccessPolicy(), currentActor,
            published::addAll, storage, clock);
    private final PublicFeedService publicFeed = new PublicFeedService(posts,
            (address, slug) -> "key-" + address, published::addAll, clock);

    private static Actor actor(long id, boolean active, ActorRole.Part part) {
        return new Actor(new UserId(id), active, Set.of(new ActorRole(part, SOCIETY)));
    }

    private PostView submit(Long towerId) {
        return posting.submit(new SubmitPostCommand(1, "No gym", "Brochure p.4 promised a gym.", towerId));
    }

    @Test
    void residentSubmitsAPostThatOnlyTheyAndModeratorsSeeUntilPublished() {
        PostView post = submit(7L);

        assertThat(post.status()).isEqualTo("PENDING_REVIEW");
        assertThat(post.tower()).isEqualTo("B2 Radiant");
        assertThat(post.slug()).matches("[a-z0-9]{12}");
        assertThat(published).hasSize(1);
        assertThat(publicFeed.list(new ListPublicPostsQuery(1, 0, 20))).isEmpty();
        assertThat(publicFeed.get(new GetPublicPostQuery(post.slug()))).isEmpty();
        assertThat(posting.myPosts(new MyPostsQuery(1, 0, 20))).extracting(PostView::id).containsExactly(post.id());

        current = ADMIN;
        assertThat(moderation.queue(new ModerationQueueQuery(1, null, 0, 20))).extracting(PostView::id)
                .containsExactly(post.id());
        moderation.decide(new ModeratePostCommand(1, post.id(), Decision.PUBLISH, null));

        assertThat(publicFeed.get(new GetPublicPostQuery(post.slug()))).hasValueSatisfying(view -> {
            assertThat(view.title()).isEqualTo("No gym");
            assertThat(view.tower()).isEqualTo("B2 Radiant");
        });
    }

    @Test
    void onlyApprovedResidentsPost() {
        current = actor(11, false, ActorRole.Part.RESIDENT);
        assertThatThrownBy(() -> submit(null)).isInstanceOf(ActionNotAllowedException.class);
        current = actor(12, true, ActorRole.Part.STAFF);
        assertThatThrownBy(() -> submit(null)).isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void theShownTowerMustBeTheAuthorsOwn() {
        assertThatThrownBy(() -> submit(8L)).isInstanceOf(InvalidPostException.class);
        assertThat(submit(null).tower()).isNull();
    }

    @Test
    void sixthPostOfTheDayIsRefused() {
        for (int i = 0; i < 5; i++) {
            submit(null);
        }
        assertThatThrownBy(() -> submit(null)).isInstanceOf(PostingLimitReachedException.class);
    }

    @Test
    void onlySocietyAdminsModerate() {
        PostView post = submit(null);
        assertThatThrownBy(() -> moderation.decide(new ModeratePostCommand(1, post.id(), Decision.PUBLISH, null)))
                .isInstanceOf(ActionNotAllowedException.class);
        current = actor(2, true, ActorRole.Part.TOWER_ADMIN);
        assertThatThrownBy(() -> moderation.queue(new ModerationQueueQuery(1, null, 0, 20)))
                .isInstanceOf(ActionNotAllowedException.class);
        current = ADMIN;
        assertThat(moderation.decide(new ModeratePostCommand(1, 999, Decision.PUBLISH, null))).isEmpty();
    }

    @Test
    void threeReportersHideAPublishedPost() {
        PostView post = submit(null);
        current = ADMIN;
        moderation.decide(new ModeratePostCommand(1, post.id(), Decision.PUBLISH, null));

        assertThat(publicFeed.report(new ReportPostCommand(post.slug(), "Names a guard", "1.1.1.1"))).isTrue();
        assertThatThrownBy(() -> publicFeed.report(new ReportPostCommand(post.slug(), "again", "1.1.1.1")))
                .isInstanceOf(InvalidPostException.class);
        publicFeed.report(new ReportPostCommand(post.slug(), "Abusive", "2.2.2.2"));
        publicFeed.report(new ReportPostCommand(post.slug(), "Fake", "3.3.3.3"));

        assertThat(publicFeed.get(new GetPublicPostQuery(post.slug()))).isEmpty();
        assertThat(publicFeed.report(new ReportPostCommand(post.slug(), "x", "4.4.4.4"))).isFalse();
        assertThat(moderation.queue(new ModerationQueueQuery(1, "HIDDEN", 0, 20))).singleElement()
                .satisfies(view -> assertThat(view.reports()).hasSize(3));
        current = RESIDENT;
        assertThat(posting.myPosts(new MyPostsQuery(1, 0, 20))).singleElement()
                .satisfies(view -> assertThat(view.reports()).isEmpty());
    }

    @Test
    void malformedLinksAreSimplyNotFound() {
        assertThat(publicFeed.get(new GetPublicPostQuery("../../etc"))).isEmpty();
        assertThat(publicFeed.report(new ReportPostCommand(null, "x", "1.1.1.1"))).isFalse();
    }
}
