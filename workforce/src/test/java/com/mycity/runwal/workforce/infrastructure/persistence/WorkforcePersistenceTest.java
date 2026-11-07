package com.mycity.runwal.workforce.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.ShiftRepository;
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

/** Runs the Flyway migration and the repository adapter on PostgreSQL. Needs Docker. */
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

    @Autowired
    private ShiftRepository shifts;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shiftsAreStoredEndedAndFoundByPeriod() {
        Shift shift = Shift.start(shifts.nextId(), RUNWAL, RAJU, NOW);
        shifts.save(shift);
        entityManager.flush();
        entityManager.clear();

        assertThat(shifts.findOpen(RUNWAL, RAJU)).hasValueSatisfying(open -> assertThat(open.id()).isEqualTo(shift.id()));
        assertThat(shifts.findAllOpen(RUNWAL)).hasSize(1);

        Shift open = shifts.findOpen(RUNWAL, RAJU).orElseThrow();
        open.end(NOW.plus(Duration.ofHours(8)));
        shifts.save(open);
        entityManager.flush();
        entityManager.clear();

        assertThat(shifts.findOpen(RUNWAL, RAJU)).isEmpty();
        assertThat(shifts.findOverlapping(RUNWAL, NOW.plus(Duration.ofHours(7)), NOW.plus(Duration.ofDays(1))))
                .singleElement().satisfies(s -> assertThat(s.endedAt()).contains(NOW.plus(Duration.ofHours(8))));
        assertThat(shifts.findOverlapping(RUNWAL, NOW.plus(Duration.ofHours(9)), NOW.plus(Duration.ofDays(1))))
                .isEmpty();
        assertThat(shifts.findOverlapping(new SocietyId(2), NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1))))
                .isEmpty();
    }

    @Test
    void theDatabaseAllowsOnlyOneOpenShiftPerStaffMember() {
        shifts.save(Shift.start(shifts.nextId(), RUNWAL, RAJU, NOW));
        entityManager.flush();

        shifts.save(Shift.start(shifts.nextId(), RUNWAL, RAJU, NOW.plusSeconds(1)));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(RuntimeException.class);
    }
}
