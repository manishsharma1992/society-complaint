package com.mycity.runwal.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.identity.domain.event.RegistrationApproved;
import com.mycity.runwal.identity.domain.event.RegistrationRejected;
import com.mycity.runwal.identity.domain.event.RegistrationSubmitted;
import com.mycity.runwal.identity.domain.event.RoleGranted;
import com.mycity.runwal.identity.domain.event.RoleRevoked;
import com.mycity.runwal.identity.domain.exception.InvalidIdentityException;
import com.mycity.runwal.identity.domain.exception.OtpRateLimitedException;
import com.mycity.runwal.identity.domain.model.otp.OtpChallenge;
import com.mycity.runwal.identity.domain.model.user.AppUser;
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
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IdentityDomainTest {

    private static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");
    private static final SocietyId RUNWAL = new SocietyId(1);
    private static final UserId ADMIN = new UserId(1);
    private static final FlatClaim B2_503 = new FlatClaim(RUNWAL, new FlatId(667), Occupancy.OWNER);

    private static AppUser newUser() {
        return AppUser.verified(new UserId(100), PhoneNumber.parse("98765 43210"), NOW);
    }

    @Nested
    class Phone {

        @ParameterizedTest
        @ValueSource(strings = {"9876543210", "98765 43210", "098765-43210", "+91 98765 43210", "919876543210"})
        void acceptsTheWaysPeopleTypeIt(String input) {
            assertThat(PhoneNumber.parse(input).value()).isEqualTo("+919876543210");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"12345", "5876543210", "+1 415 555 0100", "98765432101", "abcdefghij"})
        void rejectsNonIndianMobiles(String input) {
            assertThatThrownBy(() -> PhoneNumber.parse(input)).isInstanceOf(InvalidIdentityException.class);
        }

        @Test
        void maskedForLogs() {
            assertThat(PhoneNumber.parse("9876543210").masked()).isEqualTo("+91******3210");
        }
    }

    @Nested
    class Roles {

        @Test
        void towerAdminNeedsATowerAndSocietyWideRolesTakeNone() {
            assertThatThrownBy(() -> new RoleAssignment(Role.TOWER_ADMIN, RUNWAL, null))
                    .isInstanceOf(InvalidIdentityException.class);
            assertThatThrownBy(() -> new RoleAssignment(Role.SOCIETY_ADMIN, RUNWAL, new TowerId(7)))
                    .isInstanceOf(InvalidIdentityException.class);
            assertThat(new RoleAssignment(Role.STAFF, RUNWAL, new TowerId(7)).towerId()).isEqualTo(new TowerId(7));
            assertThat(RoleAssignment.societyWide(Role.STAFF, RUNWAL).towerId()).isNull();
            assertThatThrownBy(() -> Role.from("GUARD")).isInstanceOf(InvalidIdentityException.class);
        }
    }

    @Nested
    class SelfSignUp {

        @Test
        void registerThenApproveMakesAnActiveResident() {
            AppUser user = newUser();

            user.register("  Manish Sharma ", B2_503, NOW);
            assertThat(user.status()).isEqualTo(UserStatus.PENDING);
            assertThat(user.name()).contains("Manish Sharma");
            assertThat(user.roles()).isEmpty();

            user.approveRegistration(ADMIN, NOW.plusSeconds(60));
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.roles()).containsExactly(RoleAssignment.societyWide(Role.RESIDENT, RUNWAL));
            assertThat(user.hasRoleIn(Role.RESIDENT, RUNWAL)).isTrue();
            assertThat(user.pullDomainEvents()).containsExactly(
                    new RegistrationSubmitted(user.id(), B2_503, NOW),
                    new RegistrationApproved(user.id(), B2_503, ADMIN, NOW.plusSeconds(60)));
        }

        @Test
        void rejectedUserMayRegisterAgain() {
            AppUser user = newUser();
            user.register("Manish", B2_503, NOW);

            user.rejectRegistration(ADMIN, " Not on the owners list ", NOW);
            assertThat(user.status()).isEqualTo(UserStatus.REJECTED);
            assertThat(user.rejectionReason()).contains("Not on the owners list");

            user.register("Manish Sharma", new FlatClaim(RUNWAL, new FlatId(668), Occupancy.TENANT), NOW);
            assertThat(user.status()).isEqualTo(UserStatus.PENDING);
            assertThat(user.rejectionReason()).isEmpty();
            assertThat(user.pullDomainEvents()).hasSize(3).element(1).isInstanceOf(RegistrationRejected.class);
        }

        @Test
        void decisionsNeedAPendingRegistrationAndRejectionAReason() {
            AppUser user = newUser();
            assertThatThrownBy(() -> user.approveRegistration(ADMIN, NOW)).isInstanceOf(InvalidIdentityException.class);

            user.register("Manish", B2_503, NOW);
            assertThatThrownBy(() -> user.rejectRegistration(ADMIN, " ", NOW))
                    .isInstanceOf(InvalidIdentityException.class);
            assertThatThrownBy(() -> user.register("Again", B2_503, NOW)).isInstanceOf(InvalidIdentityException.class);
            assertThatThrownBy(() -> newUser().register(" ", B2_503, NOW)).isInstanceOf(InvalidIdentityException.class);
        }
    }

    @Nested
    class GrantingRoles {

        @Test
        void staffBecomeActiveWithTheirRole() {
            AppUser plumber = newUser();
            RoleAssignment staff = RoleAssignment.societyWide(Role.STAFF, RUNWAL);

            plumber.grantRole(staff, ADMIN, NOW);
            assertThat(plumber.status()).isEqualTo(UserStatus.ACTIVE);
            assertThatThrownBy(() -> plumber.grantRole(staff, ADMIN, NOW)).isInstanceOf(InvalidIdentityException.class);

            plumber.revokeRole(staff, ADMIN, NOW);
            assertThat(plumber.roles()).isEmpty();
            assertThatThrownBy(() -> plumber.revokeRole(staff, ADMIN, NOW)).isInstanceOf(InvalidIdentityException.class);
            assertThat(plumber.pullDomainEvents()).hasExactlyElementsOfTypes(RoleGranted.class, RoleRevoked.class);
        }

        @Test
        void pendingRegistrationIsDecidedFirst() {
            AppUser user = newUser();
            user.register("Manish", B2_503, NOW);

            assertThatThrownBy(() -> user.grantRole(RoleAssignment.societyWide(Role.STAFF, RUNWAL), ADMIN, NOW))
                    .isInstanceOf(InvalidIdentityException.class);
        }
    }

    @Nested
    class Otp {

        private final PhoneNumber phone = PhoneNumber.parse("9876543210");

        @Test
        void rightCodeOnceWithinFiveMinutes() {
            OtpChallenge challenge = OtpChallenge.issue(phone, "hash-1", NOW);

            assertThat(challenge.verify("hash-1", NOW.plus(Duration.ofMinutes(4)))).isTrue();
            assertThat(challenge.verify("hash-1", NOW.plus(Duration.ofMinutes(4)))).isFalse();
        }

        @Test
        void expiredCodeFails() {
            OtpChallenge challenge = OtpChallenge.issue(phone, "hash-1", NOW);

            assertThat(challenge.verify("hash-1", NOW.plus(OtpChallenge.VALIDITY))).isFalse();
        }

        @Test
        void fiveWrongCodesLockTheChallenge() {
            OtpChallenge challenge = OtpChallenge.issue(phone, "hash-1", NOW);
            for (int i = 0; i < OtpChallenge.MAX_ATTEMPTS; i++) {
                assertThat(challenge.verify("wrong", NOW)).isFalse();
            }

            assertThat(challenge.verify("hash-1", NOW)).isFalse();
            assertThat(challenge.failedAttempts()).isEqualTo(5);
        }

        @Test
        void newCodeAfterCooldownResetsAttempts() {
            OtpChallenge challenge = OtpChallenge.issue(phone, "hash-1", NOW);
            challenge.verify("wrong", NOW);

            assertThatThrownBy(() -> challenge.reissue("hash-2", NOW.plusSeconds(10)))
                    .isInstanceOf(OtpRateLimitedException.class)
                    .hasMessage("Please wait 21 seconds before requesting another code");

            challenge.reissue("hash-2", NOW.plusSeconds(30));
            assertThat(challenge.failedAttempts()).isZero();
            assertThat(challenge.verify("hash-1", NOW.plusSeconds(31))).isFalse();
            assertThat(challenge.verify("hash-2", NOW.plusSeconds(32))).isTrue();
        }

        @Test
        void atMostFiveCodesPerHour() {
            OtpChallenge challenge = OtpChallenge.issue(phone, "h0", NOW);
            for (int i = 1; i < OtpChallenge.MAX_SENDS_PER_HOUR; i++) {
                challenge.reissue("h" + i, NOW.plusSeconds(60L * i));
            }

            assertThatThrownBy(() -> challenge.reissue("h5", NOW.plusSeconds(600)))
                    .isInstanceOf(OtpRateLimitedException.class);
            challenge.reissue("h6", NOW.plus(Duration.ofHours(1)));
            assertThat(challenge.sendsInWindow()).isEqualTo(1);
        }
    }
}
