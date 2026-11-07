package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface PostRepository {

    PostId nextId();

    void save(Post post);

    Optional<Post> findById(SocietyId societyId, PostId id);

    Optional<Post> findBySlug(PostSlug slug);

    /** Posts of one society in these statuses; published posts newest published first, others oldest first. */
    List<Post> findBySociety(SocietyId societyId, List<PostStatus> statuses, int page, int size);

    /** Published posts of any of these societies, newest published first (the signed-in feed). */
    List<Post> findPublishedIn(Collection<SocietyId> societyIds, int page, int size);

    /** The author's own posts, newest first. */
    List<Post> findByAuthor(SocietyId societyId, UserId author, int page, int size);

    /** How many posts the author submitted at or after this moment (for the daily limit). */
    int countSubmittedSince(UserId author, Instant since);
}
