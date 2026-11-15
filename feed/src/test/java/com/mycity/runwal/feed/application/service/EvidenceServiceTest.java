package com.mycity.runwal.feed.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.feed.TestImages;
import com.mycity.runwal.feed.application.command.AttachEvidenceCommand;
import com.mycity.runwal.feed.application.command.RequestEvidenceUploadCommand;
import com.mycity.runwal.feed.application.query.EvidenceUploadView;
import com.mycity.runwal.feed.application.query.PostView;
import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.feed.domain.model.actor.Actor;
import com.mycity.runwal.feed.domain.model.actor.ActorRole;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostContent;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.infrastructure.storage.MetadataExtractorImageInspector;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The whole upload check, with real image bytes and the real inspector. */
class EvidenceServiceTest {

    private static final SocietyId SOCIETY = new SocietyId(1);
    private static final Actor AUTHOR = new Actor(new UserId(10), true,
            Set.of(new ActorRole(ActorRole.Part.RESIDENT, SOCIETY)));

    private final InMemoryPosts posts = new InMemoryPosts();
    private final FakeEvidenceStorage storage = new FakeEvidenceStorage();
    private Actor current = AUTHOR;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T06:00:00Z"), ZoneOffset.UTC);
    private final EvidenceService service = new EvidenceService(posts, () -> current, storage,
            new MetadataExtractorImageInspector(), events -> { }, clock);
    private long postId;

    @BeforeEach
    void aPendingPost() {
        Post post = Post.submit(posts.nextId(), SOCIETY, new PostSlug("abcdefgh2345"), AUTHOR.userId(),
                new PostContent("No gym", "Facts"), null, clock.instant(), 0);
        posts.save(post);
        postId = post.id().value();
    }

    private String upload(String contentType, byte[] file) {
        EvidenceUploadView link = service.requestUpload(
                new RequestEvidenceUploadCommand(1, postId, contentType, file.length)).orElseThrow();
        storage.put(link.uploadKey(), file);
        return link.uploadKey();
    }

    @Test
    void aCleanPhotoIsMovedToAKeyTheUploaderHasNoLinkFor() {
        String uploadKey = upload("image/jpeg", TestImages.jpeg(64, 48));

        PostView post = service.attach(new AttachEvidenceCommand(1, postId, uploadKey)).orElseThrow();

        assertThat(post.evidence()).singleElement().satisfies(photo -> {
            assertThat(photo.contentType()).isEqualTo("image/jpeg");
            assertThat(photo.widthPx()).isEqualTo(64);
            assertThat(photo.url()).startsWith("https://bucket.test/feed/1/" + postId + "/").doesNotContain("upload");
        });
        assertThat(storage.objects).doesNotContainKey(uploadKey).hasSize(1);
    }

    @Test
    void aJpegWithGpsExifIsRejectedAndDeleted() {
        String uploadKey = upload("image/jpeg", TestImages.jpegWithGps());

        assertThatThrownBy(() -> service.attach(new AttachEvidenceCommand(1, postId, uploadKey)))
                .isInstanceOf(EvidenceRejectedException.class)
                .hasMessageContaining("GPS location");
        assertThat(storage.objects).isEmpty();
        assertThat(posts.findBySlug(new PostSlug("abcdefgh2345")).orElseThrow().evidence()).isEmpty();
    }

    @Test
    void aPngDisguisedAsJpegIsRejectedAndDeleted() {
        String uploadKey = upload("image/jpeg", TestImages.png(64, 48));

        assertThatThrownBy(() -> service.attach(new AttachEvidenceCommand(1, postId, uploadKey)))
                .isInstanceOf(EvidenceRejectedException.class)
                .hasMessageContaining("really image/png");
        assertThat(storage.objects).isEmpty();
    }

    @Test
    void nothingUploadedIsRejected() {
        EvidenceUploadView link = service.requestUpload(
                new RequestEvidenceUploadCommand(1, postId, "image/jpeg", 1000)).orElseThrow();
        assertThatThrownBy(() -> service.attach(new AttachEvidenceCommand(1, postId, link.uploadKey())))
                .isInstanceOf(EvidenceRejectedException.class);
    }

    @Test
    void onlyTheAuthorAddsPhotos() {
        current = new Actor(new UserId(11), true, Set.of(new ActorRole(ActorRole.Part.RESIDENT, SOCIETY)));
        assertThatThrownBy(() -> service.requestUpload(new RequestEvidenceUploadCommand(1, postId, "image/jpeg", 10)))
                .isInstanceOf(ActionNotAllowedException.class);
        assertThat(service.requestUpload(new RequestEvidenceUploadCommand(1, 999, "image/jpeg", 10))).isEmpty();
    }
}
