package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.post.PostStatus;
import com.mycity.runwal.feed.domain.model.post.VideoStatus;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemoryPosts implements PostRepository {

    private final Map<PostId, Post> posts = new LinkedHashMap<>();
    private long sequence;

    @Override
    public PostId nextId() {
        return new PostId(++sequence);
    }

    @Override
    public void save(Post post) {
        posts.put(post.id(), post);
    }

    @Override
    public List<Post> findWithVideoProcessing() {
        return posts.values().stream()
                .filter(p -> p.videos().stream().anyMatch(v -> v.status() == VideoStatus.PROCESSING))
                .toList();
    }

    @Override
    public Optional<Post> findById(SocietyId societyId, PostId id) {
        return Optional.ofNullable(posts.get(id)).filter(p -> p.societyId().equals(societyId));
    }

    @Override
    public Optional<Post> findBySlug(PostSlug slug) {
        return posts.values().stream().filter(p -> p.slug().equals(slug)).findFirst();
    }

    @Override
    public List<Post> findBySociety(SocietyId societyId, List<PostStatus> statuses, int page, int size) {
        return posts.values().stream()
                .filter(p -> p.societyId().equals(societyId) && statuses.contains(p.status()))
                .skip((long) page * size).limit(size).toList();
    }

    @Override
    public List<Post> findPublishedIn(Collection<SocietyId> societyIds, int page, int size) {
        return posts.values().stream()
                .filter(p -> societyIds.contains(p.societyId()) && p.status() == PostStatus.PUBLISHED)
                .sorted(Comparator.comparing((Post p) -> p.publishedAt().orElseThrow()).reversed())
                .skip((long) page * size).limit(size).toList();
    }

    @Override
    public List<Post> findByAuthor(SocietyId societyId, UserId author, int page, int size) {
        return posts.values().stream()
                .filter(p -> p.societyId().equals(societyId) && p.author().equals(author))
                .sorted(Comparator.comparing(Post::submittedAt).reversed())
                .skip((long) page * size).limit(size).toList();
    }

    @Override
    public int countSubmittedSince(UserId author, Instant since) {
        return (int) posts.values().stream()
                .filter(p -> p.author().equals(author) && !p.submittedAt().isBefore(since))
                .count();
    }
}
