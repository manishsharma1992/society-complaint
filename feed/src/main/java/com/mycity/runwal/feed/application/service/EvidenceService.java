package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.application.command.AttachEvidenceCommand;
import com.mycity.runwal.feed.application.command.RequestEvidenceUploadCommand;
import com.mycity.runwal.feed.application.port.CurrentActor;
import com.mycity.runwal.feed.application.port.DomainEventPublisher;
import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.application.port.ImageInspector;
import com.mycity.runwal.feed.application.query.EvidenceUploadView;
import com.mycity.runwal.feed.application.query.PostView;
import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.feed.domain.model.post.Evidence;
import com.mycity.runwal.feed.domain.model.post.ImageFacts;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostRepository;
import com.mycity.runwal.feed.domain.model.post.Video;
import com.mycity.runwal.feed.domain.model.post.VideoFacts;
import com.mycity.runwal.feed.domain.model.post.VideoStatus;
import com.mycity.runwal.shared.ddd.DomainException;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Photos for a post, uploaded straight to the private bucket, never through this service:
 * 1. {@link #requestUpload} checks the post can take the photo and hands out a short-lived upload link;
 * 2. the author PUTs the file to that link;
 * 3. {@link #attach} moves the file to a key the author has no link for (so it cannot be swapped later), reads
 *    it, and adds it only if it is a real JPEG/PNG/WebP without identifying metadata. A rejected file is deleted.
 *    A video is added as waiting to be processed ({@link VideoProcessingService}); its result is recorded with
 *    {@link #videoProcessed} or {@link #videoFailed}.
 * Both methods return empty when the society has no such post.
 */
@ApplicationService
public class EvidenceService {

    static final Duration UPLOAD_LINK_VALID_FOR = Duration.ofMinutes(10);

    private final PostRepository posts;
    private final CurrentActor currentActor;
    private final EvidenceStorage storage;
    private final ImageInspector inspector;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public EvidenceService(PostRepository posts, CurrentActor currentActor, EvidenceStorage storage,
                           ImageInspector inspector, DomainEventPublisher eventPublisher, Clock clock) {
        this.posts = posts;
        this.currentActor = currentActor;
        this.storage = storage;
        this.inspector = inspector;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<EvidenceUploadView> requestUpload(RequestEvidenceUploadCommand command) {
        return findOwnPost(command.societyId(), command.postId()).map(post -> {
            String key = post.newUploadKey(command.contentType(), command.sizeBytes(), UUID.randomUUID().toString());
            String url = storage.uploadUrl(key, command.contentType(), command.sizeBytes(), UPLOAD_LINK_VALID_FOR)
                    .toString();
            return new EvidenceUploadView(key, url, command.contentType(), command.sizeBytes(),
                    clock.instant().plus(UPLOAD_LINK_VALID_FOR));
        });
    }

    @Transactional
    public Optional<PostView> attach(AttachEvidenceCommand command) {
        return findOwnPost(command.societyId(), command.postId()).map(post -> {
            String key = post.evidenceKeyFor(command.uploadKey(), UUID.randomUUID().toString());
            if (!storage.copy(command.uploadKey(), key)) {
                throw new EvidenceRejectedException("No photo has been uploaded with this link");
            }
            storage.delete(command.uploadKey());
            try {
                long size = storage.sizeOf(key)
                        .orElseThrow(() -> new EvidenceRejectedException("The photo could not be stored"));
                if (Video.isVideoKey(key)) {
                    Video.checkContent(Video.contentTypeOf(key), size);
                    post.addVideo(Video.uploaded(key, size, clock.instant()));
                } else {
                    Evidence.checkContent(Evidence.contentTypeOf(key), size);
                    ImageFacts facts = storage.read(key, inspector::inspect);
                    post.addEvidence(Evidence.inspected(key, size, facts, clock.instant()));
                }
            } catch (DomainException rejected) {
                storage.delete(key);
                throw rejected;
            }
            posts.save(post);
            eventPublisher.publish(post.pullDomainEvents());
            return PostView.forAuthor(post, storage);
        });
    }

    /** The video under this key, if it still waits to be processed. */
    @Transactional(readOnly = true)
    public Optional<Video> waitingVideo(SocietyId societyId, PostId postId, String storageKey) {
        return posts.findById(societyId, postId).flatMap(post -> post.videos().stream()
                .filter(v -> v.storageKey().equals(storageKey) && v.status() == VideoStatus.PROCESSING)
                .findFirst());
    }

    /** Posts with a video still waiting to be processed, e.g. after a restart. */
    @Transactional(readOnly = true)
    public List<Post> postsWithVideoWaiting() {
        return posts.findWithVideoProcessing();
    }

    /** Records that the video under this key has been re-encoded and can be watched. */
    @Transactional
    public void videoProcessed(SocietyId societyId, PostId postId, String storageKey, VideoFacts playable) {
        posts.findById(societyId, postId).ifPresent(post -> {
            post.videoProcessed(storageKey, playable);
            posts.save(post);
        });
    }

    /** Records that the video under this key could not be processed; the reason is shown to the author. */
    @Transactional
    public void videoFailed(SocietyId societyId, PostId postId, String storageKey, String reason) {
        posts.findById(societyId, postId).ifPresent(post -> {
            post.videoFailed(storageKey, reason);
            posts.save(post);
        });
    }

    /** The post, if it exists; only its author may add photos. */
    private Optional<Post> findOwnPost(long societyId, long postId) {
        return posts.findById(new SocietyId(societyId), new PostId(postId)).map(post -> {
            if (!post.isWrittenBy(currentActor.actor().userId())) {
                throw new ActionNotAllowedException("Only the author can add photos to a post");
            }
            return post;
        });
    }
}
