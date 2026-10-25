package com.mycity.runwal.complaint.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.complaint.application.command.AttachPhotoCommand;
import com.mycity.runwal.complaint.application.command.RequestPhotoUploadCommand;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.application.query.PhotoUploadView;
import com.mycity.runwal.complaint.application.query.PhotoView;
import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.complaint.domain.exception.PhotoNotUploadedException;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintScope;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ComplaintPhotoServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");

    private final InMemoryComplaints complaints = new InMemoryComplaints();
    private final Map<Long, Complaint> stored = complaints.stored;
    private final FakePhotoStorage bucket = new FakePhotoStorage();
    private final TestActors.Switchable caller = new TestActors.Switchable(TestActors.resident(10));
    private final ComplaintPhotoService service = new ComplaintPhotoService(complaints, new ComplaintAccessPolicy(),
            caller, bucket, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void storeOpenComplaint() {
        ComplaintCategory lift = new ComplaintCategory(new ComplaintCategoryId(5), new SocietyId(1), "Lift",
                Duration.ofHours(4), true);
        Complaint complaint = Complaint.raise(new ComplaintId(7), new SocietyId(1),
                new ComplaintLocation(ComplaintScope.COMMON_AREA, new TowerId(3), null), lift,
                new ComplaintTitle("Lift stuck"), null, new UserId(10), NOW, new SlaPolicy());
        stored.put(7L, complaint);
    }

    @Test
    void uploadLinkThenAttach() {
        PhotoUploadView upload = service.requestUpload(new RequestPhotoUploadCommand(1, 7, "image/jpeg", 250_000))
                .orElseThrow();

        assertThat(upload.storageKey()).startsWith("complaints/1/7/").endsWith(".jpg");
        assertThat(upload.uploadUrl()).contains(upload.storageKey()).contains("expires=600");
        assertThat(upload.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        assertThat(stored.get(7L).photos()).isEmpty();

        bucket.upload(upload.storageKey(), 250_000);
        ComplaintView view = service.attach(new AttachPhotoCommand(1, 7, upload.storageKey())).orElseThrow();

        assertThat(view.photos()).singleElement().satisfies(photo -> {
            assertThat(photo.url()).contains(upload.storageKey()).contains("view&expires=900");
            assertThat(photo.contentType()).isEqualTo("image/jpeg");
            assertThat(photo.sizeBytes()).isEqualTo(250_000);
            assertThat(photo.uploadedBy()).isEqualTo(10);
            assertThat(photo.uploadedAt()).isEqualTo(NOW);
        });
        assertThat(stored.get(7L).photos()).hasSize(1);
    }

    @Test
    void sizeAndTypeComeFromTheBucketAndKeyNotFromTheClient() {
        String key = service.requestUpload(new RequestPhotoUploadCommand(1, 7, "image/png", 100_000))
                .orElseThrow().storageKey();
        bucket.upload(key, 99_000);

        List<PhotoView> photos = service.attach(new AttachPhotoCommand(1, 7, key)).orElseThrow().photos();

        assertThat(photos.getFirst().sizeBytes()).isEqualTo(99_000);
        assertThat(photos.getFirst().contentType()).isEqualTo("image/png");
    }

    @Test
    void attachingBeforeUploadingFails() {
        assertThatThrownBy(() -> service.attach(new AttachPhotoCommand(1, 7, "complaints/1/7/missing.jpg")))
                .isInstanceOf(PhotoNotUploadedException.class);
        assertThat(stored.get(7L).photos()).isEmpty();
    }

    @Test
    void photoUploadedForAnotherComplaintCannotBeAttached() {
        bucket.upload("complaints/1/8/other.jpg", 1000);

        assertThatThrownBy(() -> service.attach(new AttachPhotoCommand(1, 7, "complaints/1/8/other.jpg")))
                .isInstanceOf(InvalidComplaintException.class);
    }

    @Test
    void noUploadLinkForUnsupportedType() {
        assertThatThrownBy(() -> service.requestUpload(new RequestPhotoUploadCommand(1, 7, "image/gif", 1000)))
                .isInstanceOf(InvalidComplaintException.class);
    }

    @Test
    void otherResidentsCannotAddPhotos() {
        caller.become(TestActors.resident(11));

        assertThatThrownBy(() -> service.requestUpload(new RequestPhotoUploadCommand(1, 7, "image/jpeg", 1000)))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void complaintOfAnotherSocietyIsNotFound() {
        assertThat(service.requestUpload(new RequestPhotoUploadCommand(2, 7, "image/jpeg", 1000))).isEmpty();
        assertThat(service.attach(new AttachPhotoCommand(2, 7, "complaints/2/7/a.jpg"))).isEmpty();
    }
}
