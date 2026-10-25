package com.mycity.runwal.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.identity.domain.model.otp.OtpChallenge;
import com.mycity.runwal.identity.domain.model.otp.OtpChallengeRepository;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.AppUserRepository;
import com.mycity.runwal.identity.domain.model.user.FlatClaim;
import com.mycity.runwal.identity.domain.model.user.Occupancy;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.identity.domain.model.user.Role;
import com.mycity.runwal.identity.domain.model.user.RoleAssignment;
import com.mycity.runwal.identity.domain.model.user.UserStatus;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Runs the Flyway migrations (including the demo users) and the repository adapters on PostgreSQL. Needs Docker. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({IdentitySchemaConfig.class, AppUserRepositoryAdapter.class, OtpChallengeRepositoryAdapter.class})
class IdentityPersistenceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private static final SocietyId RUNWAL = new SocietyId(1);
    // PostgreSQL stores microseconds
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private AppUserRepository users;

    @Autowired
    private OtpChallengeRepository challenges;

    @Autowired
    private EntityManager entityManager;

    @Test
    void demoUsersHaveTheirRoles() {
        AppUser towerAdmin = users.findByPhone(PhoneNumber.parse("9000000002")).orElseThrow();

        assertThat(towerAdmin.id()).isEqualTo(new UserId(2));
        assertThat(towerAdmin.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(towerAdmin.roles()).containsExactly(new RoleAssignment(Role.TOWER_ADMIN, RUNWAL, new TowerId(7)));
        assertThat(users.findById(new UserId(1)).orElseThrow().hasRoleIn(Role.SOCIETY_ADMIN, RUNWAL)).isTrue();
    }

    @Test
    void usersByIdAndByRole() {
        assertThat(users.findAllById(List.of(new UserId(3), new UserId(4), new UserId(999))))
                .extracting(AppUser::id).containsExactlyInAnyOrder(new UserId(3), new UserId(4));
        assertThat(users.findWithRole(Role.STAFF, RUNWAL)).extracting(u -> u.name().orElseThrow())
                .containsExactly("Demo Staff");
        assertThat(users.findWithRole(Role.STAFF, new SocietyId(2))).isEmpty();
    }

    @Test
    void signUpIsStoredStepByStep() {
        AppUser user = AppUser.verified(users.nextId(), PhoneNumber.parse("9876543210"), NOW);
        assertThat(user.id().value()).isGreaterThanOrEqualTo(100);
        users.save(user);
        flushAndClear();

        AppUser loaded = users.findByPhone(PhoneNumber.parse("+919876543210")).orElseThrow();
        loaded.register("Manish Sharma", new FlatClaim(RUNWAL, new FlatId(667), Occupancy.OWNER), NOW);
        users.save(loaded);
        flushAndClear();
        assertThat(users.findPendingRegistrations(RUNWAL)).extracting(AppUser::id).containsExactly(user.id());

        AppUser pending = users.findById(user.id()).orElseThrow();
        pending.approveRegistration(new UserId(1), NOW);
        pending.grantRole(new RoleAssignment(Role.STAFF, RUNWAL, new TowerId(7)), new UserId(1), NOW);
        users.save(pending);
        flushAndClear();

        AppUser active = users.findById(user.id()).orElseThrow();
        assertThat(active.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(active.name()).contains("Manish Sharma");
        assertThat(active.claim()).contains(new FlatClaim(RUNWAL, new FlatId(667), Occupancy.OWNER));
        assertThat(active.roles()).containsExactly(RoleAssignment.societyWide(Role.RESIDENT, RUNWAL),
                new RoleAssignment(Role.STAFF, RUNWAL, new TowerId(7)));
        assertThat(users.findPendingRegistrations(RUNWAL)).isEmpty();

        active.revokeRole(new RoleAssignment(Role.STAFF, RUNWAL, new TowerId(7)), new UserId(1), NOW);
        users.save(active);
        flushAndClear();
        assertThat(users.findById(user.id()).orElseThrow().roles())
                .containsExactly(RoleAssignment.societyWide(Role.RESIDENT, RUNWAL));
    }

    @Test
    void otpChallengeKeepsItsCounters() {
        PhoneNumber phone = PhoneNumber.parse("9876543210");
        OtpChallenge challenge = OtpChallenge.issue(phone, "hash-1", NOW);
        challenge.verify("wrong", NOW);
        challenges.save(challenge);
        flushAndClear();

        OtpChallenge loaded = challenges.findByPhone(phone).orElseThrow();
        assertThat(loaded.failedAttempts()).isEqualTo(1);
        loaded.reissue("hash-2", NOW.plusSeconds(60));
        challenges.save(loaded);
        flushAndClear();

        OtpChallenge reloaded = challenges.findByPhone(phone).orElseThrow();
        assertThat(reloaded.sendsInWindow()).isEqualTo(2);
        assertThat(reloaded.verify("hash-2", NOW.plusSeconds(61))).isTrue();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
