package com.mycity.runwal.feed.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.feed.application.command.AttachEvidenceCommand;
import com.mycity.runwal.feed.application.command.RequestEvidenceUploadCommand;
import com.mycity.runwal.feed.application.port.VideoTranscoder;
import com.mycity.runwal.feed.application.query.PostView;
import com.mycity.runwal.feed.application.query.PublicPostView;
import com.mycity.runwal.feed.domain.event.VideoUploaded;
import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.feed.domain.model.actor.Actor;
import com.mycity.runwal.feed.domain.model.actor.ActorRole;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostContent;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.post.Video;
import com.mycity.runwal.feed.domain.model.post.VideoFacts;
import com.mycity.runwal.feed.domain.model.post.VideoStatus;
import com.mycity.runwal.feed.domain.model.reaction.ReactionCounts;
import com.mycity.runwal.feed.infrastructure.storage.MetadataExtractorImageInspector;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Upload, processing and its outcomes, with a stand-in for ffmpeg. */
class VideoProcessingServiceTest {

    private static final SocietyId SOCIETY = new SocietyId(1);
    private static final Actor AUTHOR = new Actor(new UserId(10), true,
            Set.of(new ActorRole(ActorRole.Part.RESIDENT, SOCIETY)));
    private static final VideoFacts PLAYABLE = new VideoFacts(478, 850, 6_000);

    private final InMemoryPosts posts = new InMemoryPosts();
    private final FakeEvidenceStorage storage = new FakeEvidenceStorage();
    private final List<DomainEvent> published = new ArrayList<>();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T06:00:00Z"), ZoneOffset.UTC);
    private final EvidenceService evidence = new EvidenceService(posts, () -> AUTHOR, storage,
            new MetadataExtractorImageInspector(), published::addAll, clock);
    private VideoTranscoder transcoder = VideoProcessingServiceTest::encodes;
    private PostId postId;

    /** Like ffmpeg on a good video: writes an MP4 and a poster, hands them over, deletes them. */
    private static VideoFacts encodes(java.io.InputStream original, java.util.function.BiConsumer<Path, Path> store) {
        try {
            Path mp4 = Files.createTempFile("play", ".mp4");
            Path poster = Files.createTempFile("poster", ".jpg");
            try {
                Files.write(mp4, new byte[] {1, 2, 3});
                Files.write(poster, new byte[] {4, 5});
                store.accept(mp4, poster);
            } finally {
                Files.delete(mp4);
                Files.delete(poster);
            }
            return PLAYABLE;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private VideoProcessingService service() {
        return new VideoProcessingService(storage, (in, store) -> transcoder.transcode(in, store), evidence);
    }

    @BeforeEach
    void aPendingPost() {
        Post post = Post.submit(posts.nextId(), SOCIETY, new PostSlug("abcdefgh2345"), AUTHOR.userId(),
                new PostContent("Basement floods", "Water pours into the car park."), null, clock.instant(), 0);
        post.pullDomainEvents(); // published when it was submitted
        posts.save(post);
        postId = post.id();
    }

    private Video attachVideo() {
        var link = evidence.requestUpload(
                new RequestEvidenceUploadCommand(1, postId.value(), "video/quicktime", 1000)).orElseThrow();
        storage.put(link.uploadKey(), new byte[1000]);
        evidence.attach(new AttachEvidenceCommand(1, postId.value(), link.uploadKey()));
        return video();
    }

    private Video video() {
        return posts.findById(SOCIETY, postId).orElseThrow().videos().getFirst();
    }

    @Test
    void anAttachedVideoWaitsAndAsksToBeProcessed() {
        Video video = attachVideo();

        assertThat(video.status()).isEqualTo(VideoStatus.PROCESSING);
        assertThat(published).singleElement().isInstanceOf(VideoUploaded.class)
                .extracting(e -> ((VideoUploaded) e).storageKey()).isEqualTo(video.storageKey());
        assertThat(storage.objects).containsOnlyKeys(video.storageKey());
    }

    @Test
    void aProcessedVideoIsReadyAndOnlyTheCleanCopyAndPosterAreKept() {
        Video video = attachVideo();

        service().process(SOCIETY, postId, video.storageKey());

        assertThat(video().status()).isEqualTo(VideoStatus.READY);
        assertThat(video().facts()).isEqualTo(PLAYABLE);
        assertThat(storage.objects).containsOnlyKeys(video.playableKey(), video.posterKey());
    }

    @Test
    void aRejectedVideoFailsWithItsReasonAndNothingIsKept() {
        Video video = attachVideo();
        transcoder = (in, store) -> {
            throw new EvidenceRejectedException("A video can be at most 60 seconds long");
        };

        service().process(SOCIETY, postId, video.storageKey());

        assertThat(video().status()).isEqualTo(VideoStatus.FAILED);
        assertThat(video().failureReason()).isEqualTo("A video can be at most 60 seconds long");
        assertThat(storage.objects).isEmpty();
    }

    @Test
    void aToolFailureFailsTheVideoWithAPlainMessage() {
        Video video = attachVideo();
        transcoder = (in, store) -> {
            throw new IllegalStateException("ffmpeg failed with exit code 1: /tmp/feed-media/... details");
        };

        service().process(SOCIETY, postId, video.storageKey());

        assertThat(video().status()).isEqualTo(VideoStatus.FAILED);
        assertThat(video().failureReason()).isEqualTo(VideoProcessingService.COULD_NOT_PROCESS);
        assertThat(storage.objects).isEmpty();
    }

    @Test
    void aShutdownLeavesTheVideoWaitingWithItsOriginal() {
        Video video = attachVideo();
        transcoder = (in, store) -> {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ffmpeg was interrupted");
        };

        try {
            assertThatThrownBy(() -> service().process(SOCIETY, postId, video.storageKey()))
                    .hasMessageContaining("interrupted");
        } finally {
            Thread.interrupted(); // clear the flag for the next test
        }
        assertThat(video().status()).isEqualTo(VideoStatus.PROCESSING);
        assertThat(storage.objects).containsKey(video.storageKey());
    }

    @Test
    void thePublicSeesOnlyAReadyVideoAndOnlyItsCleanCopy() {
        Video video = attachVideo();
        Post post = posts.findById(SOCIETY, postId).orElseThrow();
        assertThat(PublicPostView.of(post, ReactionCounts.NONE, storage).videos()).isEmpty();
        assertThat(PostView.forAuthor(post, storage).videos()).singleElement()
                .satisfies(v -> assertThat(v.status()).isEqualTo("PROCESSING"))
                .satisfies(v -> assertThat(v.url()).isNull());

        service().process(SOCIETY, postId, video.storageKey());

        Post ready = posts.findById(SOCIETY, postId).orElseThrow();
        assertThat(PublicPostView.of(ready, ReactionCounts.NONE, storage).videos()).singleElement().satisfies(v -> {
            assertThat(v.url()).contains(video.playableKey()).doesNotContain(video.storageKey());
            assertThat(v.posterUrl()).contains(video.posterKey());
            assertThat(v.durationMs()).isEqualTo(6_000L);
        });
    }

    @Test
    void videosLeftWaitingAreProcessedOnceAndProcessedOnesAreLeftAlone() {
        Video video = attachVideo();

        assertThat(service().processAllWaiting()).isEqualTo(1);
        assertThat(video().status()).isEqualTo(VideoStatus.READY);
        assertThat(service().processAllWaiting()).isZero();

        service().process(SOCIETY, postId, video.storageKey()); // a late duplicate trigger does nothing
        assertThat(video().status()).isEqualTo(VideoStatus.READY);
    }
}
