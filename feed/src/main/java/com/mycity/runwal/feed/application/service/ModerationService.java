package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.application.command.ModeratePostCommand;
import com.mycity.runwal.feed.application.port.CurrentActor;
import com.mycity.runwal.feed.application.port.DomainEventPublisher;
import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.application.query.ModerationQueueQuery;
import com.mycity.runwal.feed.application.query.PostView;
import com.mycity.runwal.feed.domain.model.actor.Actor;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.PostStatus;
import com.mycity.runwal.feed.domain.service.FeedAccessPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** Moderators review posts before they become public, and hide or remove published ones. */
@ApplicationService
public class ModerationService {

    private final PostRepository posts;
    private final FeedAccessPolicy accessPolicy;
    private final CurrentActor currentActor;
    private final DomainEventPublisher eventPublisher;
    private final EvidenceStorage evidenceStorage;
    private final Clock clock;

    public ModerationService(PostRepository posts, FeedAccessPolicy accessPolicy, CurrentActor currentActor,
                             DomainEventPublisher eventPublisher, EvidenceStorage evidenceStorage, Clock clock) {
        this.posts = posts;
        this.accessPolicy = accessPolicy;
        this.currentActor = currentActor;
        this.eventPublisher = eventPublisher;
        this.evidenceStorage = evidenceStorage;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PostView> queue(ModerationQueueQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        requireModerator(societyId);
        PostStatus status = query.status() == null ? PostStatus.PENDING_REVIEW : PostStatus.from(query.status());
        return posts.findBySociety(societyId, List.of(status), query.page(), query.size()).stream()
                .map(post -> PostView.forModerator(post, evidenceStorage))
                .toList();
    }

    /** Returns empty when the society has no such post. */
    @Transactional
    public Optional<PostView> decide(ModeratePostCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        Actor moderator = requireModerator(societyId);
        return posts.findById(societyId, new PostId(command.postId())).map(post -> {
            Instant now = clock.instant();
            switch (command.decision()) {
                case PUBLISH -> post.publish(moderator.userId(), now);
                case REJECT -> post.reject(moderator.userId(), now, command.reason());
                case HIDE -> post.hide(moderator.userId(), now, command.reason());
                case REMOVE -> post.remove(moderator.userId(), now, command.reason());
            }
            posts.save(post);
            eventPublisher.publish(post.pullDomainEvents());
            return PostView.forModerator(post, evidenceStorage);
        });
    }

    private Actor requireModerator(SocietyId societyId) {
        Actor actor = currentActor.actor();
        if (!accessPolicy.mayModerate(actor, societyId)) {
            throw new ActionNotAllowedException("Only the society admin can moderate the feed");
        }
        return actor;
    }
}
