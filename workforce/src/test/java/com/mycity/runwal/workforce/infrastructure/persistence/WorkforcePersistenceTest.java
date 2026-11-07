package com.mycity.runwal.workforce.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.model.shift.ClockEvidence;
import com.mycity.runwal.workforce.domain.model.shift.GeoPoint;
import com.mycity.runwal.workforce.domain.model.shift.Location;
import com.mycity.runwal.workforce.domain.model.shift.Placement;
import com.mycity.runwal.workforce.domain.model.shift.SelfieKey;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.ShiftRepository;
import com.mycity.runwal.workforce.domain.model.shift.ShiftReview;
import com.mycity.runwal.workforce.domain.model.shift.Site;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Runs the Flyway migrations and the repository adapter on PostgreSQL. Needs Docker. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({WorkforceSchemaConfig.class, ShiftRepositoryAdapter.class})
class WorkforcePersistenceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private static final SocietyId RUNWAL = new SocietyId(1);
    private static final UserId RAJU = new UserId(30);
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);
    private static final Site SITE = new Site(new GeoPoint(19.186062, 73.074812), 300);
    private static final Location GATE = new Location(new GeoPoint(19.1865, 73.0750), 12);

    @Autowired
    private ShiftRepository shifts;

    @Autowired
    private EntityManager entityManager;

    private static ClockEvidence selfie(Instant at, Location where) {
        return ClockEvidence.at(SITE, SelfieKey.issue(RUNWAL, RAJU, at.minusSeconds(20), "abcdef12"), where);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shiftsAreStoredWithEvidenceEndedReviewedAndFoundByPeriod() {
        Shift shift = Shift.start(shifts.nextId(), RUNWAL, RAJU, NOW, selfie(NOW, GATE));
        shifts.save(shift);
        flushAndClear();

        assertThat(shifts.findOpen(RUNWAL, RAJU)).hasValueSatisfying(open -> {
            assertThat(open.id()).isEqualTo(shift.id());
            assertThat(open.startEvidence()).contains(shift.startEvidence().orElseThrow());
        });
        assertThat(shifts.findAllOpen(RUNWAL)).hasSize(1);

        Shift open = shifts.findOpen(RUNWAL, RAJU).orElseThrow();
        Instant end = NOW.plus(Duration.ofHours(8));
        open.end(end, selfie(end, null));
        open.dispute(new UserId(2), end.plusSeconds(60), "No location at clock-out");
        shifts.save(open);
        flushAndClear();

        Shift loaded = shifts.findById(RUNWAL, shift.id()).orElseThrow();
        assertThat(loaded.endEvidence()).hasValueSatisfying(e -> {
            assertThat(e.check().placement()).isEqualTo(Placement.NO_LOCATION);
            assertThat(e.where()).isEmpty();
            assertThat(e.selfieKey()).isPresent();
        });
        assertThat(loaded.review()).hasValueSatisfying(r -> {
            assertThat(r.decision()).isEqualTo(ShiftReview.Decision.DISPUTED);
            assertThat(r.reason()).isEqualTo("No location at clock-out");
        });
        assertThat(shifts.findById(new SocietyId(2), shift.id())).isEmpty();
        assertThat(shifts.findOpen(RUNWAL, RAJU)).isEmpty();
        assertThat(shifts.findOverlapping(RUNWAL, NOW.plus(Duration.ofHours(7)), NOW.plus(Duration.ofDays(1))))
                .singleElement().satisfies(s -> assertThat(s.endedAt()).contains(end));
        assertThat(shifts.findOverlapping(RUNWAL, NOW.plus(Duration.ofHours(9)), NOW.plus(Duration.ofDays(1))))
                .isEmpty();
    }

    @Test
    void oldShiftsHoldingEvidenceAreFoundAndForgottenEvidenceIsStored() {
        Instant old = NOW.minus(Duration.ofDays(100));
        Shift shift = Shift.start(shifts.nextId(), RUNWAL, RAJU, old, selfie(old, GATE));
        shift.end(old.plus(Duration.ofHours(8)), selfie(old.plus(Duration.ofHours(8)), GATE));
        shifts.save(shift);
        flushAndClear();

        assertThat(shifts.findHoldingEvidenceStartedBefore(NOW.minus(Duration.ofDays(90)), 10)).hasSize(1);
        assertThat(shifts.findHoldingEvidenceStartedBefore(old, 10)).isEmpty();

        Shift loaded = shifts.findById(RUNWAL, shift.id()).orElseThrow();
        loaded.forgetEvidence();
        shifts.save(loaded);
        flushAndClear();

        assertThat(shifts.findHoldingEvidenceStartedBefore(NOW, 10)).isEmpty();
        assertThat(shifts.findById(RUNWAL, shift.id()).orElseThrow().startEvidence())
                .hasValueSatisfying(e -> assertThat(e.check().placement()).isEqualTo(Placement.INSIDE));
    }

    @Test
    void theDatabaseAllowsOnlyOneOpenShiftPerStaffMember() {
        shifts.save(Shift.start(shifts.nextId(), RUNWAL, RAJU, NOW, selfie(NOW, GATE)));
        entityManager.flush();

        shifts.save(Shift.start(shifts.nextId(), RUNWAL, RAJU, NOW.plusSeconds(1), selfie(NOW.plusSeconds(1), GATE)));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(RuntimeException.class);
    }
}
