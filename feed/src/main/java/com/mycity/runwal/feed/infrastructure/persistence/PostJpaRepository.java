package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.post.PostStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {

    Optional<PostJpaEntity> findByIdAndSocietyId(long id, long societyId);

    Optional<PostJpaEntity> findBySlug(String slug);

    @Query("""
            select p from PostJpaEntity p
            where p.societyId = :societyId and p.status in :statuses
            order by p.publishedAt desc nulls last, p.submittedAt asc, p.id asc""")
    List<PostJpaEntity> search(@Param("societyId") long societyId,
                               @Param("statuses") Collection<PostStatus> statuses,
                               Pageable pageable);

    @Query("""
            select p from PostJpaEntity p
            where p.societyId in :societyIds and p.status = :published
            order by p.publishedAt desc, p.id desc""")
    List<PostJpaEntity> findPublishedIn(@Param("societyIds") Collection<Long> societyIds,
                                        @Param("published") PostStatus published, Pageable pageable);

    @Query("""
            select p from PostJpaEntity p
            where p.societyId = :societyId and p.authorId = :authorId
            order by p.submittedAt desc, p.id desc""")
    List<PostJpaEntity> findByAuthor(@Param("societyId") long societyId, @Param("authorId") long authorId,
                                     Pageable pageable);

    @Query("select count(p) from PostJpaEntity p where p.authorId = :authorId and p.submittedAt >= :since")
    long countSubmittedSince(@Param("authorId") long authorId, @Param("since") Instant since);

    @Query(value = "select nextval('feed.post_id_seq')", nativeQuery = true)
    long nextId();
}
