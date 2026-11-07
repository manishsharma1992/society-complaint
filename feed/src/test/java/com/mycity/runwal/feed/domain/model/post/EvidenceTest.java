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

class EvidenceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T06:00:00Z");
    private static final ImageFacts CLEAN_JPEG = new ImageFacts("image/jpeg", 1600, 1200, Set.of());

    private static Post pending() {
        return Post.submit(new PostId(42), new SocietyId(1), new PostSlug("abcdefgh2345"), new UserId(10),
                new PostContent("No gym", "Facts"), null, NOW, 0);
    }

    private static Evidence photo(Post post, String name) {
        return Evidence.inspected(post.evidenceKeyPrefix() + name + ".jpg", 300_000, CLEAN_JPEG, NOW);
    }

    @Test
    void uploadsGoToTheirOwnKeyAndMoveToTheirFinalKey() {
        Post post = pending();

        String upload = post.newUploadKey("image/jpeg", 300_000, "u1");
        assertThat(upload).isEqualTo("feed/1/42/upload/u1.jpg");
        assertThat(post.evidenceKeyFor(upload, "f1")).isEqualTo("feed/1/42/f1.jpg");
        assertThatThrownBy(() -> post.evidenceKeyFor("feed/1/43/upload/u1.jpg", "f1"))
                .isInstanceOf(InvalidPostException.class);
        assertThatThrownBy(() -> post.evidenceKeyFor("feed/1/42/upload/../../x.jpg", "f1"))
                .isInstanceOf(InvalidPostException.class);
    }

    @Test
    void onlyJpegPngWebpUpToFiveMegabytes() {
        Post post = pending();
        assertThatThrownBy(() -> post.newUploadKey("image/gif", 1000, "u")).isInstanceOf(EvidenceRejectedException.class);
        assertThatThrownBy(() -> post.newUploadKey("image/jpeg", 5L * 1024 * 1024 + 1, "u"))
                .isInstanceOf(EvidenceRejectedException.class);
        assertThat(post.newUploadKey("image/webp", 1000, "u")).endsWith(".webp");
    }

    @Test
    void aJpegWithGpsIsRejectedWithAClearReason() {
        ImageFacts withGps = new ImageFacts("image/jpeg", 100, 100, Set.of("GPS location"));
        assertThatThrownBy(() -> Evidence.inspected("feed/1/42/f.jpg", 1000, withGps, NOW))
                .isInstanceOf(EvidenceRejectedException.class)
                .hasMessageContaining("GPS location")
                .hasMessageContaining("could identify you");
    }

    @Test
    void aFileDisguisedByItsExtensionIsRejected() {
        ImageFacts reallyPng = new ImageFacts("image/png", 100, 100, Set.of());
        assertThatThrownBy(() -> Evidence.inspected("feed/1/42/f.jpg", 1000, reallyPng, NOW))
                .isInstanceOf(EvidenceRejectedException.class)
                .hasMessageContaining("really image/png");
        assertThatThrownBy(() -> Evidence.inspected("feed/1/42/f.jpg", 1000, ImageFacts.notAnImage(), NOW))
                .isInstanceOf(EvidenceRejectedException.class)
                .hasMessageContaining("not a JPEG, PNG or WebP");
    }

    @Test
    void moreThanFiftyMegapixelsIsRejected() {
        ImageFacts huge = new ImageFacts("image/jpeg", 10_000, 5_001, Set.of());
        assertThatThrownBy(() -> Evidence.inspected("feed/1/42/f.jpg", 1000, huge, NOW))
                .isInstanceOf(EvidenceRejectedException.class)
                .hasMessageContaining("50 megapixels");
    }

    @Test
    void atMostFivePhotosOnlyWhilePendingAndOnlyFromThisPost() {
        Post post = pending();
        for (int i = 0; i < 5; i++) {
            post.addEvidence(photo(post, "p" + i));
        }
        assertThat(post.evidence()).hasSize(5);
        assertThatThrownBy(() -> post.addEvidence(photo(post, "p5"))).isInstanceOf(InvalidPostException.class);
        assertThatThrownBy(() -> post.newUploadKey("image/jpeg", 1000, "u")).isInstanceOf(InvalidPostException.class);

        Post other = pending();
        assertThatThrownBy(() -> other.addEvidence(Evidence.inspected("feed/1/99/x.jpg", 1000, CLEAN_JPEG, NOW)))
                .isInstanceOf(InvalidPostException.class);
        assertThatThrownBy(() -> other.addEvidence(photo(other, "upload/x"))).isInstanceOf(InvalidPostException.class);
        other.addEvidence(photo(other, "a"));
        assertThatThrownBy(() -> other.addEvidence(photo(other, "a"))).isInstanceOf(InvalidPostException.class);

        other.publish(new UserId(1), NOW);
        assertThatThrownBy(() -> other.addEvidence(photo(other, "b"))).isInstanceOf(InvalidPostException.class);
    }
}
