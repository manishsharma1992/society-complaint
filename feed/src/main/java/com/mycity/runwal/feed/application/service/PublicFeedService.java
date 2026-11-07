package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.application.command.ReportPostCommand;
import com.mycity.runwal.feed.application.port.DomainEventPublisher;
import com.mycity.runwal.feed.application.port.ReporterFingerprint;
import com.mycity.runwal.feed.application.query.GetPublicPostQuery;
import com.mycity.runwal.feed.application.query.ListPublicPostsQuery;
import com.mycity.runwal.feed.application.query.PublicPostView;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostReport;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.post.PostStatus;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** What everyone can do without logging in: read published posts and report them. */
@ApplicationService
public class PublicFeedService {

    private final PostRepository posts;
    private final ReporterFingerprint reporterFingerprint;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public PublicFeedService(PostRepository posts, ReporterFingerprint reporterFingerprint,
                             DomainEventPublisher eventPublisher, Clock clock) {
        this.posts = posts;
        this.reporterFingerprint = reporterFingerprint;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PublicPostView> list(ListPublicPostsQuery query) {
        return posts.findBySociety(new SocietyId(query.societyId()), List.of(PostStatus.PUBLISHED), query.page(),
                        query.size())
                .stream()
                .map(PublicPostView::of)
                .toList();
    }

    /** Empty unless the post is published. */
    @Transactional(readOnly = true)
    public Optional<PublicPostView> get(GetPublicPostQuery query) {
        return findPublished(query.slug()).map(PublicPostView::of);
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
