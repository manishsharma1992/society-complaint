package com.mycity.runwal.feed.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.feed.domain.model.post.Evidence;
import com.mycity.runwal.feed.domain.model.post.ImageFacts;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostContent;
import com.mycity.runwal.feed.domain.model.post.PostReport;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.post.PostStatus;
import com.mycity.runwal.feed.domain.model.post.ShownTower;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Runs the Flyway migration and the repository adapter on PostgreSQL. Needs Docker. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({FeedSchemaConfig.class, PostRepositoryAdapter.class})
class FeedPersistenceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private static final SocietyId RUNWAL = new SocietyId(1);
    private static final UserId AUTHOR = new UserId(10);
    private static final UserId MODERATOR = new UserId(1);
    // PostgreSQL stores microseconds
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private PostRepository posts;

    @Autowired
    private EntityManager entityManager;

    private Post newPost(String slug) {
        return Post.submit(posts.nextId(), RUNWAL, new PostSlug(slug), AUTHOR,
                new PostContent("No gym", "Brochure p.4 promised a gym."),
                new ShownTower(new TowerId(7), "B2 Radiant"), NOW, 0);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findsPublishedPostsOfSeveralSocietiesNewestFirst() {
        Post older = newPost("older0000001");
        older.publish(MODERATOR, NOW.minusSeconds(60));
        Post newer = newPost("newer0000001");
        newer.publish(MODERATOR, NOW);
        Post pending = newPost("pending00001");
        posts.save(older);
        posts.save(newer);
        posts.save(pending);
        entityManager.flush();
        entityManager.clear();

        assertThat(posts.findPublishedIn(List.of(RUNWAL, new SocietyId(2)), 0, 10))
                .extracting(p -> p.slug().value()).containsExactly("newer0000001", "older0000001");
        assertThat(posts.findPublishedIn(List.of(new SocietyId(2)), 0, 10)).isEmpty();
        assertThat(posts.findPublishedIn(List.of(), 0, 10)).isEmpty();
    }

    @Test
    void savesAndReloadsAPostWithItsModerationLogAndReports() {
        Post post = newPost("abcdefgh2345");
        posts.save(post);
        flushAndClear();

        Post loaded = posts.findBySlug(new PostSlug("abcdefgh2345")).orElseThrow();
        loaded.addEvidence(Evidence.inspected(loaded.evidenceKeyPrefix() + "a.jpg", 300_000,
                new ImageFacts("image/jpeg", 1600, 1200, Set.of()), NOW));
        loaded.publish(MODERATOR, NOW.plusSeconds(1));
        loaded.report(new PostReport("k1", "Names a guard", NOW.plusSeconds(2)));
        posts.save(loaded);
        flushAndClear();

        Post reloaded = posts.findById(RUNWAL, post.id()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(reloaded.shownTower()).contains(new ShownTower(new TowerId(7), "B2 Radiant"));
        assertThat(reloaded.publishedAt()).contains(NOW.plusSeconds(1));
        assertThat(reloaded.moderationLog()).extracting(e -> e.to()).containsExactly(PostStatus.PENDING_REVIEW,
                PostStatus.PUBLISHED);
        assertThat(reloaded.openReports()).extracting(PostReport::reason).containsExactly("Names a guard");
        assertThat(reloaded.evidence()).singleElement().satisfies(photo -> {
            assertThat(photo.storageKey()).isEqualTo(reloaded.evidenceKeyPrefix() + "a.jpg");
            assertThat(photo.widthPx()).isEqualTo(1600);
            assertThat(photo.uploadedAt()).isEqualTo(NOW);
        });

        reloaded.hide(MODERATOR, NOW.plusSeconds(3), "Checking");
        reloaded.publish(MODERATOR, NOW.plusSeconds(4));
        posts.save(reloaded);
        flushAndClear();
        Post restored = posts.findById(RUNWAL, post.id()).orElseThrow();
        assertThat(restored.openReports()).isEmpty();
        assertThat(restored.moderationLog()).hasSize(4);
    }

    @Test
    void listsByStatusAndAuthorAndCountsTheDailyLimit() {
        Post pending = newPost("pending00001");
        Post live = newPost("published001");
        live.publish(MODERATOR, NOW);
        posts.save(pending);
        posts.save(live);
        flushAndClear();

        assertThat(posts.findBySociety(RUNWAL, List.of(PostStatus.PUBLISHED), 0, 20))
                .extracting(p -> p.slug().value()).containsExactly("published001");
        assertThat(posts.findByAuthor(RUNWAL, AUTHOR, 0, 20)).hasSize(2);
        assertThat(posts.countSubmittedSince(AUTHOR, NOW.minusSeconds(60))).isEqualTo(2);
        assertThat(posts.countSubmittedSince(AUTHOR, NOW.plusSeconds(60))).isZero();
    }
}
