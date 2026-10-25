package com.mycity.runwal.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.identity.application.command.ApproveRegistrationCommand;
import com.mycity.runwal.identity.application.command.GrantRoleCommand;
import com.mycity.runwal.identity.application.command.RegisterCommand;
import com.mycity.runwal.identity.application.command.RejectRegistrationCommand;
import com.mycity.runwal.identity.application.command.RequestOtpCommand;
import com.mycity.runwal.identity.application.command.RevokeRoleCommand;
import com.mycity.runwal.identity.application.command.VerifyOtpCommand;
import com.mycity.runwal.identity.application.query.ListPendingRegistrationsQuery;
import com.mycity.runwal.identity.application.query.OtpSentView;
import com.mycity.runwal.identity.application.query.RoleView;
import com.mycity.runwal.identity.application.query.SessionView;
import com.mycity.runwal.identity.application.query.UserView;
import com.mycity.runwal.identity.domain.event.RegistrationApproved;
import com.mycity.runwal.identity.domain.exception.InvalidIdentityException;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.identity.domain.model.user.Role;
import com.mycity.runwal.identity.domain.model.user.RoleAssignment;
import com.mycity.runwal.identity.domain.model.user.UserStatus;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.shared.ddd.DomainEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IdentityServicesTest {

    private static final Instant START = Instant.parse("2026-10-25T08:00:00Z");
    private static final String PHONE = "98765 43210";

    private final MutableClock clock = new MutableClock(START);
    private final IdentityFakes.Users users = new IdentityFakes.Users();
    private final IdentityFakes.Challenges challenges = new IdentityFakes.Challenges();
    private final Map<PhoneNumber, String> sms = new LinkedHashMap<>();
    private final List<DomainEvent> published = new ArrayList<>();
    private int codes;

    private final OtpLoginService login = new OtpLoginService(challenges, users,
            phone -> String.format("%06d", 111111 * (++codes % 9 + 1)),
            (phone, code) -> "hash(" + phone.value() + ":" + code + ")",
            sms::put,
            (user, issuedAt, expiresAt) -> "token-for-" + user.id().value() + "-" + user.status(),
            clock);
    private long caller;
    private final RegistrationService registrations = new RegistrationService(users,
            (societyId, flatId) -> societyId.value() == 1 && flatId.value() == 667, () -> new UserId(caller),
            published::addAll, clock);
    private final RoleService roles = new RoleService(users, () -> new UserId(caller), published::addAll, clock);
    private static final Instant LOGIN_ENDS = START.plus(Duration.ofDays(30));
    private final SessionService sessions = new SessionService(users, () -> new UserId(caller), () -> LOGIN_ENDS,
            (user, issuedAt, expiresAt) -> "token-for-" + user.id().value() + "-" + user.status() + "-until-" + expiresAt,
            clock);

    IdentityServicesTest() {
        users.save(new AppUser(new UserId(1), PhoneNumber.parse("9000000001"), START, "Admin", UserStatus.ACTIVE,
                null, null, List.of(RoleAssignment.societyWide(Role.SOCIETY_ADMIN, new SocietyId(1)))));
    }

    /** Logs in the test's phone number and makes it the caller. */
    private long loggedInUser() {
        caller = logIn().user().id();
        return caller;
    }

    private void asAdmin() {
        caller = 1;
    }

    private SessionView logIn() {
        login.requestOtp(new RequestOtpCommand(PHONE));
        return login.verify(new VerifyOtpCommand(PHONE, sms.get(PhoneNumber.parse(PHONE))));
    }

    @Test
    void newNumberLogsInAsVerifiedUser() {
        OtpSentView sent = login.requestOtp(new RequestOtpCommand(PHONE));
        assertThat(sent.sentTo()).isEqualTo("+91******3210");
        assertThat(sent.expiresInSeconds()).isEqualTo(300);

        SessionView session = login.verify(new VerifyOtpCommand("+919876543210", sms.values().iterator().next()));

        assertThat(session.user().id()).isEqualTo(100);
        assertThat(session.user().status()).isEqualTo("VERIFIED");
        assertThat(session.token()).isEqualTo("token-for-100-VERIFIED");
        assertThat(session.expiresAt()).isEqualTo(START.plus(Duration.ofDays(30)));
    }

    @Test
    void sameNumberIsTheSameUserNextTime() {
        long first = logIn().user().id();
        clock.advance(Duration.ofMinutes(1));

        assertThat(logIn().user().id()).isEqualTo(first);
        assertThat(users.stored).hasSize(2);
    }

    @Test
    void wrongCodeIsRejectedAndCounted() {
        login.requestOtp(new RequestOtpCommand(PHONE));

        assertThatThrownBy(() -> login.verify(new VerifyOtpCommand(PHONE, "000000")))
                .isInstanceOf(LoginFailedException.class);
        assertThat(challenges.stored.get(PhoneNumber.parse(PHONE)).failedAttempts()).isEqualTo(1);
        assertThat(users.stored).hasSize(1);
    }

    @Test
    void verifyWithoutRequestingFails() {
        assertThatThrownBy(() -> login.verify(new VerifyOtpCommand(PHONE, "111111")))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
    void codesAreRateLimited() {
        login.requestOtp(new RequestOtpCommand(PHONE));

        assertThatThrownBy(() -> login.requestOtp(new RequestOtpCommand(PHONE)))
                .isInstanceOf(TooManyCodesException.class);
    }

    @Test
    void signUpApproveAndLogInAsResident() {
        long userId = loggedInUser();

        UserView pending = registrations.register(new RegisterCommand("Manish Sharma", 1, 667, "OWNER"))
                .orElseThrow();
        assertThat(pending.status()).isEqualTo("PENDING");
        assertThat(pending.claim().flatId()).isEqualTo(667);
        asAdmin();
        assertThat(registrations.pending(new ListPendingRegistrationsQuery(1)))
                .extracting(UserView::name).containsExactly("Manish Sharma");
        assertThatThrownBy(() -> registrations.pending(new ListPendingRegistrationsQuery(2)))
                .isInstanceOf(ActionNotAllowedException.class);

        UserView active = registrations.approve(new ApproveRegistrationCommand(1, userId)).orElseThrow();
        assertThat(active.status()).isEqualTo("ACTIVE");
        assertThat(active.roles()).containsExactly(new RoleView("RESIDENT", 1, null));
        assertThat(published).last().isInstanceOf(RegistrationApproved.class);

        caller = userId;
        SessionView renewed = sessions.renew().orElseThrow();
        assertThat(renewed.token()).isEqualTo("token-for-" + userId + "-ACTIVE-until-" + LOGIN_ENDS);
        assertThat(renewed.expiresAt()).isEqualTo(LOGIN_ENDS);
        assertThat(renewed.user().status()).isEqualTo("ACTIVE");

        clock.advance(Duration.ofMinutes(1));
        assertThat(logIn().token()).isEqualTo("token-for-" + userId + "-ACTIVE");
    }

    @Test
    void registrationNeedsAnExistingFlat() {
        long userId = loggedInUser();

        assertThatThrownBy(() -> registrations.register(new RegisterCommand("Manish", 1, 99999, "OWNER")))
                .isInstanceOf(InvalidIdentityException.class)
                .hasMessage("Society 1 has no flat 99999");
    }

    @Test
    void registrationIsDecidedOnlyInItsOwnSociety() {
        long userId = loggedInUser();
        registrations.register(new RegisterCommand("Manish", 1, 667, "TENANT"));
        users.save(new AppUser(new UserId(2), PhoneNumber.parse("9000000009"), START, "Other admin",
                UserStatus.ACTIVE, null, null, List.of(RoleAssignment.societyWide(Role.SOCIETY_ADMIN, new SocietyId(2)))));
        caller = 2;

        assertThat(registrations.approve(new ApproveRegistrationCommand(2, userId))).isEmpty();
        asAdmin();
        assertThat(registrations.reject(new RejectRegistrationCommand(1, userId, "Not a tenant here")))
                .hasValueSatisfying(u -> assertThat(u.status()).isEqualTo("REJECTED"));
    }

    @Test
    void onlyTheSocietyAdminDecidesAndGrants() {
        long userId = loggedInUser();
        registrations.register(new RegisterCommand("Manish", 1, 667, "OWNER"));

        assertThatThrownBy(() -> registrations.approve(new ApproveRegistrationCommand(1, userId)))
                .isInstanceOf(ActionNotAllowedException.class)
                .hasMessage("Only the society admin can approve registrations");
        assertThatThrownBy(() -> registrations.pending(new ListPendingRegistrationsQuery(1)))
                .isInstanceOf(ActionNotAllowedException.class);
        assertThatThrownBy(() -> roles.grant(new GrantRoleCommand(1, userId, "SOCIETY_ADMIN", null)))
                .isInstanceOf(ActionNotAllowedException.class);
        asAdmin();
        assertThatThrownBy(() -> registrations.approve(new ApproveRegistrationCommand(2, userId)))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void meIsTheLoggedInUser() {
        long userId = loggedInUser();

        assertThat(registrations.me()).hasValueSatisfying(u -> assertThat(u.id()).isEqualTo(userId));
    }

    @Test
    void staffGetTheirRole() {
        long userId = loggedInUser();
        asAdmin();

        UserView staff = roles.grant(new GrantRoleCommand(1, userId, "STAFF", 7L)).orElseThrow();
        assertThat(staff.status()).isEqualTo("ACTIVE");
        assertThat(staff.roles()).containsExactly(new RoleView("STAFF", 1, 7L));

        assertThat(roles.revoke(new RevokeRoleCommand(1, userId, "STAFF", 7L)).orElseThrow().roles()).isEmpty();
        assertThat(roles.grant(new GrantRoleCommand(1, 12345, "STAFF", null))).isEmpty();
    }

    /** A clock the test can move forward (OTP cooldowns). */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
