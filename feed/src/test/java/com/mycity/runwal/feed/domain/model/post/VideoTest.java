package com.mycity.runwal.feed.domain.model.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoTest {

    private static final Instant NOW = Instant.parse("2026-11-01T06:00:00Z");
    private static final UserId MODERATOR = new UserId(1);
    private static final VideoFacts PLAYABLE = new VideoFacts(720, 1280, 6_000);

    private static Post pending() {
        return Post.submit(new PostId(42), new SocietyId(1), new PostSlug("abcdefgh2345"), new UserId(10),
                new PostContent("Basement floods", "Water pours into the car park when it rains."), null, NOW, 0);
    }

    private static Video uploaded(Post post, String name) {
        return Video.uploaded(post.evidenceKeyPrefix() + name + ".mov", 1_200_000, NOW);
    }

    @Test
    void mp4MovAndWebmUpToFiftyMegabytesGetTheirOwnUploadKey() {
        Post post = pending();

        String upload = post.newUploadKey("video/quicktime", 1_200_000, "u1");
        assertThat(upload).isEqualTo("feed/1/42/upload/u1.mov");
        assertThat(post.evidenceKeyFor(upload, "f1")).isEqualTo("feed/1/42/f1.mov");
        assertThat(post.newUploadKey("video/mp4", 1000, "u")).endsWith(".mp4");
        assertThat(post.newUploadKey("video/webm", 1000, "u")).endsWith(".webm");
        assertThatThrownBy(() -> post.newUploadKey("video/x-msvideo", 1000, "u"))
                .isInstanceOf(EvidenceRejectedException.class);
        assertThatThrownBy(() -> post.newUploadKey("video/mp4", 50L * 1024 * 1024 + 1, "u"))
                .isInstanceOf(EvidenceRejectedException.class)
                .hasMessageContaining("50 MB");
    }

    @Test
    void anUploadedVideoIsProcessedBeforeItCanBeWatched() {
        Post post = pending();
        Video video = uploaded(post, "f1");
        post.addVideo(video);

        assertThat(post.videos()).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(VideoStatus.PROCESSING);
            assertThat(v.contentType()).isEqualTo("video/quicktime");
            assertThat(v.playableKey()).isEqualTo("feed/1/42/f1-play.mp4");
            assertThat(v.posterKey()).isEqualTo("feed/1/42/f1-poster.jpg");
        });

        post.videoProcessed(video.storageKey(), PLAYABLE);
        assertThat(post.videos().getFirst().status()).isEqualTo(VideoStatus.READY);
        assertThat(post.videos().getFirst().facts()).isEqualTo(PLAYABLE);
        assertThatThrownBy(() -> post.videoFailed(video.storageKey(), "late"))
                .isInstanceOf(InvalidPostException.class);
    }

    @Test
    void aFailedVideoKeepsItsReasonForTheAuthor() {
        Post post = pending();
        Video video = uploaded(post, "f1");
        post.addVideo(video);

        post.videoFailed(video.storageKey(), "A video can be at most 60 seconds long");

        assertThat(post.videos().getFirst().status()).isEqualTo(VideoStatus.FAILED);
        assertThat(post.videos().getFirst().failureReason()).contains("60 seconds");
        Video again = uploaded(post, "f2");
        post.addVideo(again);
        assertThat(post.videos()).extracting(Video::status).containsExactly(VideoStatus.FAILED, VideoStatus.PROCESSING);
        assertThatThrownBy(() -> post.videoProcessed("feed/1/42/other.mov", PLAYABLE))
                .isInstanceOf(InvalidPostException.class);
    }

    @Test
    void addingAVideoAsksForItToBeProcessed() {
        Post post = pending();
        post.pullDomainEvents();
        post.addVideo(uploaded(post, "f1"));

        assertThat(post.pullDomainEvents()).singleElement()
                .isEqualTo(new com.mycity.runwal.feed.domain.event.VideoUploaded(new PostId(42), new SocietyId(1),
                        "feed/1/42/f1.mov", NOW));
    }

    @Test
    void aPostCannotBePublishedWhileItsVideoIsProcessing() {
        Post post = pending();
        Video video = uploaded(post, "f1");
        post.addVideo(video);

        assertThatThrownBy(() -> post.publish(MODERATOR, NOW))
                .isInstanceOf(InvalidPostException.class)
                .hasMessageContaining("still being processed");

        post.videoProcessed(video.storageKey(), PLAYABLE);
        post.publish(MODERATOR, NOW);
        assertThat(post.isPublished()).isTrue();
    }

    @Test
    void oneVideoPerPostAndItCountsTowardsTheFiveItems() {
        Post post = pending();
        post.addVideo(uploaded(post, "f1"));

        assertThatThrownBy(() -> post.addVideo(uploaded(post, "f2"))).isInstanceOf(InvalidPostException.class);
        assertThatThrownBy(() -> post.newUploadKey("video/mp4", 1000, "u")).isInstanceOf(InvalidPostException.class);

        ImageFacts jpeg = new ImageFacts("image/jpeg", 100, 100, Set.of());
        for (int i = 0; i < Post.MAX_EVIDENCE - 1; i++) {
            post.addEvidence(Evidence.inspected(post.evidenceKeyPrefix() + "p" + i + ".jpg", 1000, jpeg, NOW));
        }
        assertThatThrownBy(() -> post.newUploadKey("image/jpeg", 1000, "u"))
                .isInstanceOf(InvalidPostException.class)
                .hasMessageContaining("photos and videos");
    }

    @Test
    void videosAreAddedOnlyWhileWaitingForReviewAndOnlyUnderThisPostsKeys() {
        Post post = pending();
        assertThatThrownBy(() -> post.addVideo(Video.uploaded("feed/1/43/f1.mov", 1000, NOW)))
                .isInstanceOf(InvalidPostException.class);
        assertThatThrownBy(() -> post.addVideo(Video.uploaded("feed/1/42/upload/f1.mov", 1000, NOW)))
                .isInstanceOf(InvalidPostException.class);

        post.publish(MODERATOR, NOW);
        assertThatThrownBy(() -> post.addVideo(uploaded(post, "f1"))).isInstanceOf(InvalidPostException.class);
    }

    @Test
    void longerThanSixtySecondsOrNotAVideoIsRejected() {
        assertThatThrownBy(() -> new VideoFacts(720, 1280, 60_001))
                .isInstanceOf(EvidenceRejectedException.class)
                .hasMessageContaining("60 seconds");
        assertThatThrownBy(() -> new VideoFacts(0, 0, 1000)).isInstanceOf(EvidenceRejectedException.class);
        assertThatThrownBy(() -> Video.uploaded("feed/1/42/f1.gif", 1000, NOW))
                .isInstanceOf(EvidenceRejectedException.class);
    }
}
