package com.mycity.runwal.complaint.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryRepository;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintComment;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintPhoto;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintRepository;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.complaint.domain.model.complaint.StatusChangeSnapshot;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Runs the Flyway migrations and the repository adapters against a real PostgreSQL. Needs Docker. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({ComplaintSchemaConfig.class, ComplaintRepositoryAdapter.class, ComplaintCategoryRepositoryAdapter.class})
class ComplaintPersistenceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private static final SocietyId SOCIETY = new SocietyId(1);
    private static final ComplaintCategoryId PLUMBING = new ComplaintCategoryId(1);
    private static final UserId RESIDENT = new UserId(10);
    private static final UserId ADMIN = new UserId(20);
    private static final UserId STAFF = new UserId(30);
    // PostgreSQL stores microseconds
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private ComplaintRepository complaints;

    @Autowired
    private ComplaintCategoryRepository categories;

    @Autowired
    private EntityManager entityManager;

    @Test
    void seededCategoriesAreScopedBySociety() {
        ComplaintCategory plumbing = categories.findById(SOCIETY, PLUMBING).orElseThrow();

        assertThat(plumbing.name()).isEqualTo("Plumbing");
        assertThat(plumbing.sla()).isEqualTo(Duration.ofHours(24));
        assertThat(categories.findById(new SocietyId(2), PLUMBING)).isEmpty();
        assertThat(categories.findActiveBySociety(SOCIETY)).extracting(ComplaintCategory::name).containsExactly(
                "Plumbing", "Electrical", "Lift", "Water supply", "Security", "Housekeeping", "Parking",
                "Garden & amenities", "Other");
    }

    @Test
    void storesAndReloadsAComplaintWithItsHistory() {
        Complaint complaint = raise(ComplaintLocation.flat(new TowerId(3), new FlatId(1204)), NOW);
        complaints.save(complaint);
        flushAndClear();

        Complaint loaded = complaints.findById(SOCIETY, complaint.id()).orElseThrow();
        loaded.assignTo(STAFF, ADMIN, NOW.plusSeconds(60));
        complaints.save(loaded);
        flushAndClear();

        Complaint reloaded = complaints.findById(SOCIETY, complaint.id()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(ComplaintStatus.ASSIGNED);
        assertThat(reloaded.assignedTo()).contains(STAFF);
        assertThat(reloaded.location()).isEqualTo(complaint.location());
        assertThat(reloaded.dueAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(reloaded.description()).contains("Since yesterday");
        assertThat(reloaded.history()).containsExactly(
                new StatusChangeSnapshot(1, null, ComplaintStatus.OPEN, RESIDENT, NOW, null),
                new StatusChangeSnapshot(2, ComplaintStatus.OPEN, ComplaintStatus.ASSIGNED, ADMIN,
                        NOW.plusSeconds(60), null));
        assertThat(complaints.findById(new SocietyId(2), complaint.id())).isEmpty();
    }

    @Test
    void storesPhotosInTheOrderTheyWereAdded() {
        Complaint complaint = raise(ComplaintLocation.commonArea(new TowerId(3)), NOW);
        ComplaintPhoto first = new ComplaintPhoto(complaint.photoKeyPrefix() + "a.jpg", "image/jpeg", 250_000,
                RESIDENT, NOW);
        ComplaintPhoto second = new ComplaintPhoto(complaint.photoKeyPrefix() + "b.webp", "image/webp", 180_000,
                STAFF, NOW.plusSeconds(60));
        complaint.addPhoto(first);
        complaints.save(complaint);
        flushAndClear();

        Complaint loaded = complaints.findById(SOCIETY, complaint.id()).orElseThrow();
        assertThat(loaded.photos()).containsExactly(first);
        loaded.addPhoto(second);
        complaints.save(loaded);
        flushAndClear();

        assertThat(complaints.findById(SOCIETY, complaint.id()).orElseThrow().photos())
                .containsExactly(first, second);
        assertThat(complaints.findBySociety(SOCIETY, null, null, null, 0, 10).getFirst().photos()).hasSize(2);
    }

    @Test
    void listsBySocietyWithOptionalFiltersNewestFirst() {
        Complaint tower3 = raise(ComplaintLocation.commonArea(new TowerId(3)), NOW);
        Complaint societyWide = raise(ComplaintLocation.commonArea(null), NOW.plusSeconds(1));
        tower3.assignTo(STAFF, ADMIN, NOW.plusSeconds(2));
        complaints.save(tower3);
        complaints.save(societyWide);
        flushAndClear();

        assertThat(complaints.findBySociety(SOCIETY, null, null, null, 0, 10))
                .extracting(Complaint::id).containsExactly(societyWide.id(), tower3.id());
        assertThat(complaints.findBySociety(SOCIETY, null, new TowerId(3), null, 0, 10))
                .extracting(Complaint::id).containsExactly(tower3.id());
        assertThat(complaints.findBySociety(SOCIETY, ComplaintStatus.OPEN, null, null, 0, 1)).hasSize(1);
        assertThat(complaints.findBySociety(SOCIETY, ComplaintStatus.CLOSED, null, null, 0, 10)).isEmpty();
        assertThat(complaints.findBySociety(new SocietyId(2), null, null, null, 0, 10)).isEmpty();
        assertThat(complaints.findBySociety(SOCIETY, null, null, STAFF, 0, 10))
                .extracting(Complaint::id).containsExactly(tower3.id());
        assertThat(complaints.findBySociety(SOCIETY, null, null, RESIDENT, 0, 10)).isEmpty();
    }

    @Test
    void commentsAreAppendedAcrossSaves() {
        Complaint complaint = raise(ComplaintLocation.commonArea(new TowerId(3)), NOW);
        complaint.comment(RESIDENT, "Lift stuck again", NOW);
        complaints.save(complaint);
        flushAndClear();

        Complaint loaded = complaints.findById(SOCIETY, complaint.id()).orElseThrow();
        loaded.comment(STAFF, "Technician on the way", NOW.plusSeconds(60));
        complaints.save(loaded);
        flushAndClear();

        assertThat(complaints.findById(SOCIETY, complaint.id()).orElseThrow().comments()).containsExactly(
                new ComplaintComment(1, RESIDENT, "Lift stuck again", NOW),
                new ComplaintComment(2, STAFF, "Technician on the way", NOW.plusSeconds(60)));
    }

    @Test
    void residentSeesOwnFlatsOwnRaisedAndCommonAreaComplaints() {
        Complaint ownFlat = raise(ComplaintLocation.flat(new TowerId(7), new FlatId(667)), NOW);
        Complaint neighbour = raise(ComplaintLocation.flat(new TowerId(7), new FlatId(668)), NOW.plusSeconds(1));
        Complaint commonArea = raise(ComplaintLocation.commonArea(new TowerId(7)), NOW.plusSeconds(2));
        complaints.save(ownFlat);
        complaints.save(neighbour);
        complaints.save(commonArea);
        flushAndClear();

        assertThat(complaints.findVisibleTo(SOCIETY, STAFF, Set.of(new FlatId(667)), null, null, null, 0, 10))
                .extracting(Complaint::id).containsExactly(commonArea.id(), ownFlat.id());
        assertThat(complaints.findVisibleTo(SOCIETY, RESIDENT, Set.of(), null, null, null, 0, 10))
                .extracting(Complaint::id).containsExactly(commonArea.id(), neighbour.id(), ownFlat.id());
        assertThat(complaints.findVisibleTo(SOCIETY, STAFF, Set.of(), ComplaintStatus.OPEN, null, null, 0, 10))
                .extracting(Complaint::id).containsExactly(commonArea.id());
    }

    private Complaint raise(ComplaintLocation location, Instant raisedAt) {
        ComplaintId id = complaints.nextId();
        return Complaint.raise(id, SOCIETY, location, categories.findById(SOCIETY, PLUMBING).orElseThrow(),
                new ComplaintTitle("Leak " + id.value()), "Since yesterday", RESIDENT,
                raisedAt, new SlaPolicy());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
