package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.application.command.ReactCommand;
import com.mycity.runwal.feed.application.command.ReportPostCommand;
import com.mycity.runwal.feed.application.command.TakeBackReactionCommand;
import com.mycity.runwal.feed.application.port.DomainEventPublisher;
import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.application.port.ReporterFingerprint;
import com.mycity.runwal.feed.application.query.GetPublicPostQuery;
import com.mycity.runwal.feed.application.query.ListPublicPostsQuery;
import com.mycity.runwal.feed.application.query.PublicPostView;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostReport;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.post.PostStatus;
import com.mycity.runwal.feed.domain.model.reaction.Emoji;
import com.mycity.runwal.feed.domain.model.reaction.Reaction;
import com.mycity.runwal.feed.domain.model.reaction.ReactionCounts;
import com.mycity.runwal.feed.domain.model.reaction.ReactionRepository;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** What everyone can do without logging in: read published posts, react to them with an emoji, report them. */
@ApplicationService
public class PublicFeedService {

    private final PostRepository posts;
    private final ReporterFingerprint reporterFingerprint;
    private final DomainEventPublisher eventPublisher;
    private final ReactionRepository reactions;
    private final EvidenceStorage storage;
    private final Clock clock;

    public PublicFeedService(PostRepository posts, ReporterFingerprint reporterFingerprint,
                             DomainEventPublisher eventPublisher, ReactionRepository reactions,
                             EvidenceStorage storage, Clock clock) {
        this.posts = posts;
        this.reporterFingerprint = reporterFingerprint;
        this.eventPublisher = eventPublisher;
        this.reactions = reactions;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PublicPostView> list(ListPublicPostsQuery query) {
        List<Post> page = posts.findBySociety(new SocietyId(query.societyId()), List.of(PostStatus.PUBLISHED),
                query.page(), query.size());
        Map<PostId, ReactionCounts> counts = reactions.countsFor(page.stream().map(Post::id).toList());
        return page.stream()
                .map(post -> PublicPostView.of(post, counts.getOrDefault(post.id(), ReactionCounts.NONE), storage))
                .toList();
    }

    /** Empty unless the post is published. */
    @Transactional(readOnly = true)
    public Optional<PublicPostView> get(GetPublicPostQuery query) {
        return findPublished(query.slug()).map(post -> PublicPostView.of(post, countsOf(post), storage));
    }

    /**
     * Replaces the caller's earlier reaction to the post, if any. Returns the post's counts afterwards, or empty
     * when there is no published post with this link.
     */
    @Transactional
    public Optional<Map<String, Integer>> react(ReactCommand command) {
        Emoji emoji = emojiNamed(command.emoji());
        return findPublished(command.slug()).map(post -> {
            String reactorKey = reporterFingerprint.keyFor(command.reactorAddress(), post.slug());
            reactions.save(Reaction.to(post, reactorKey, emoji, clock.instant()));
            return PublicPostView.byName(countsOf(post));
        });
    }

    /** Returns the post's counts afterwards, or empty when there is no published post with this link. */
    @Transactional
    public Optional<Map<String, Integer>> takeBackReaction(TakeBackReactionCommand command) {
        return findPublished(command.slug()).map(post -> {
            reactions.remove(post.id(), reporterFingerprint.keyFor(command.reactorAddress(), post.slug()));
            return PublicPostView.byName(countsOf(post));
        });
    }

    private ReactionCounts countsOf(Post post) {
        return reactions.countsFor(List.of(post.id())).getOrDefault(post.id(), ReactionCounts.NONE);
    }

    private static Emoji emojiNamed(String name) {
        for (Emoji emoji : Emoji.values()) {
            if (emoji.name().equals(name)) {
                return emoji;
            }
        }
        throw new InvalidPostException("Unknown emoji: choose one of THUMBS_UP, HEART, SURPRISED, SAD, ANGRY");
    }

    /** Returns false when there is no published post with this link. */
    @Transactional
    public boolean report(ReportPostCommand command) {
        Optional<Post> found = findPublished(command.slug());
        found.ifPresent(post -> {
            String reporterKey = reporterFingerprint.keyFor(command.reporterAddress(), post.slug());
            post.report(new PostReport(reporterKey, command.reason(), clock.instant()));
            posts.save(post);
            eventPublisher.publish(post.pullDomainEvents());
        });
        return found.isPresent();
    }

    private Optional<Post> findPublished(String slug) {
        PostSlug postSlug;
        try {
            postSlug = new PostSlug(slug);
        } catch (InvalidPostException malformed) {
            return Optional.empty();
        }
        return posts.findBySlug(postSlug).filter(Post::isPublished);
    }
}
