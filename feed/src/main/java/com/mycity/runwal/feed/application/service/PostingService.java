package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.application.command.SubmitPostCommand;
import com.mycity.runwal.feed.application.port.CurrentActor;
import com.mycity.runwal.feed.application.port.DomainEventPublisher;
import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.application.port.ResidentTowers;
import com.mycity.runwal.feed.application.query.MyPostsQuery;
import com.mycity.runwal.feed.application.query.PostView;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.model.actor.Actor;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostContent;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.post.ShownTower;
import com.mycity.runwal.feed.domain.service.FeedAccessPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/** Residents submit posts and follow their own posts through moderation. */
@ApplicationService
public class PostingService {

    private static final String SLUG_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PostRepository posts;
    private final FeedAccessPolicy accessPolicy;
    private final CurrentActor currentActor;
    private final ResidentTowers residentTowers;
    private final DomainEventPublisher eventPublisher;
    private final EvidenceStorage evidenceStorage;
    private final Clock clock;

    public PostingService(PostRepository posts, FeedAccessPolicy accessPolicy, CurrentActor currentActor,
                          ResidentTowers residentTowers, DomainEventPublisher eventPublisher,
                          EvidenceStorage evidenceStorage, Clock clock) {
        this.posts = posts;
        this.accessPolicy = accessPolicy;
        this.currentActor = currentActor;
        this.residentTowers = residentTowers;
        this.eventPublisher = eventPublisher;
        this.evidenceStorage = evidenceStorage;
        this.clock = clock;
    }

    @Transactional
    public PostView submit(SubmitPostCommand command) {
        Actor actor = currentActor.actor();
        SocietyId societyId = new SocietyId(command.societyId());
        if (!accessPolicy.mayPost(actor, societyId)) {
            throw new ActionNotAllowedException("Only approved residents of this society can post");
        }
        ShownTower tower = null;
        if (command.showTowerId() != null) {
            tower = residentTowers.towerOf(societyId, actor.userId(), new TowerId(command.showTowerId()))
                    .orElseThrow(() -> new InvalidPostException("You can only show a tower you live in"));
        }
        Instant now = clock.instant();
        int postsInLastDay = posts.countSubmittedSince(actor.userId(), now.minus(Duration.ofDays(1)));
        Post post = Post.submit(posts.nextId(), societyId, newSlug(), actor.userId(),
                new PostContent(command.title(), command.body()), tower, now, postsInLastDay);
        posts.save(post);
        eventPublisher.publish(post.pullDomainEvents());
        return PostView.forAuthor(post, evidenceStorage);
    }

    @Transactional(readOnly = true)
    public List<PostView> myPosts(MyPostsQuery query) {
        Actor actor = currentActor.actor();
        return posts.findByAuthor(new SocietyId(query.societyId()), actor.userId(), query.page(), query.size())
                .stream()
                .map(post -> PostView.forAuthor(post, evidenceStorage))
                .toList();
    }

    private static PostSlug newSlug() {
        StringBuilder slug = new StringBuilder(PostSlug.LENGTH);
        for (int i = 0; i < PostSlug.LENGTH; i++) {
            slug.append(SLUG_ALPHABET.charAt(RANDOM.nextInt(SLUG_ALPHABET.length())));
        }
        return new PostSlug(slug.toString());
    }
}
