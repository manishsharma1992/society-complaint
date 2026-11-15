package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.application.port.CurrentActor;
import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.application.port.NearbySocieties;
import com.mycity.runwal.feed.application.query.CommunityFeedQuery;
import com.mycity.runwal.feed.application.query.FeedPostQuery;
import com.mycity.runwal.feed.application.query.FeedPostView;
import com.mycity.runwal.feed.application.query.NeighbourView;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.feed.domain.model.actor.Actor;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.reaction.ReactionCounts;
import com.mycity.runwal.feed.domain.model.reaction.ReactionRepository;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * The signed-in feed, the app's home: published posts from the viewer's society and the societies near it, each
 * labelled with its society. Only approved members of a society see it (until the legal pages make it public).
 */
@ApplicationService
public class CommunityFeedService {

    private final PostRepository posts;
    private final NearbySocieties nearby;
    private final CurrentActor currentActor;
    private final EvidenceStorage storage;
    private final ReactionRepository reactions;

    public CommunityFeedService(PostRepository posts, NearbySocieties nearby, CurrentActor currentActor,
                                EvidenceStorage storage, ReactionRepository reactions) {
        this.posts = posts;
        this.nearby = nearby;
        this.currentActor = currentActor;
        this.storage = storage;
        this.reactions = reactions;
    }

    @Transactional(readOnly = true)
    public List<FeedPostView> feed(CommunityFeedQuery query) {
        Map<SocietyId, String> names = neighboursOfMember(new SocietyId(query.societyId()));
        List<Post> page = posts.findPublishedIn(names.keySet(), query.page(), query.size());
        Map<PostId, ReactionCounts> counts = reactions.countsFor(page.stream().map(Post::id).toList());
        return page.stream()
                .map(post -> FeedPostView.of(post, names.get(post.societyId()),
                        counts.getOrDefault(post.id(), ReactionCounts.NONE), storage))
                .toList();
    }

    /** Empty unless the post is published and from the viewer's society or a nearby one. */
    @Transactional(readOnly = true)
    public Optional<FeedPostView> post(FeedPostQuery query) {
        Map<SocietyId, String> names = neighboursOfMember(new SocietyId(query.societyId()));
        PostSlug slug;
        try {
            slug = new PostSlug(query.slug());
        } catch (InvalidPostException malformed) {
            return Optional.empty();
        }
        return posts.findBySlug(slug)
                .filter(Post::isPublished)
                .filter(post -> names.containsKey(post.societyId()))
                .map(post -> FeedPostView.of(post, names.get(post.societyId()),
                        reactions.countsFor(List.of(post.id())).getOrDefault(post.id(), ReactionCounts.NONE),
                        storage));
    }

    private Map<SocietyId, String> neighboursOfMember(SocietyId societyId) {
        Actor actor = currentActor.actor();
        if (!actor.active() || !actor.belongsTo(societyId)) {
            throw new ActionNotAllowedException("Only approved members of this society see its feed");
        }
        return nearby.of(societyId).stream()
                .collect(Collectors.toMap(NeighbourView::societyId, NeighbourView::name, (a, b) -> a));
    }
}
