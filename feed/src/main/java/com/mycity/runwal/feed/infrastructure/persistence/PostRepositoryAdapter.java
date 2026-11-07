package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.post.PostStatus;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class PostRepositoryAdapter implements PostRepository {

    private final PostJpaRepository jpaRepository;
    private final EntityManager entityManager;

    PostRepositoryAdapter(PostJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public PostId nextId() {
        return new PostId(jpaRepository.nextId());
    }

    @Override
    public void save(Post post) {
        jpaRepository.findById(post.id().value()).ifPresentOrElse(
                existing -> PostMapper.updateEntity(post, existing),
                () -> entityManager.persist(PostMapper.toNewEntity(post)));
    }

    @Override
    public Optional<Post> findById(SocietyId societyId, PostId id) {
        return jpaRepository.findByIdAndSocietyId(id.value(), societyId.value()).map(PostMapper::toDomain);
    }

    @Override
    public Optional<Post> findBySlug(PostSlug slug) {
        return jpaRepository.findBySlug(slug.value()).map(PostMapper::toDomain);
    }

    @Override
    public List<Post> findBySociety(SocietyId societyId, List<PostStatus> statuses, int page, int size) {
        return jpaRepository.search(societyId.value(), statuses, PageRequest.of(page, size)).stream()
                .map(PostMapper::toDomain)
                .toList();
    }

    @Override
    public List<Post> findByAuthor(SocietyId societyId, UserId author, int page, int size) {
        return jpaRepository.findByAuthor(societyId.value(), author.value(), PageRequest.of(page, size)).stream()
                .map(PostMapper::toDomain)
                .toList();
    }

    @Override
    public int countSubmittedSince(UserId author, Instant since) {
        return Math.toIntExact(jpaRepository.countSubmittedSince(author.value(), since));
    }
}
