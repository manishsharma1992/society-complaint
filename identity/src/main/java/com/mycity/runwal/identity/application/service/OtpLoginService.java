package com.mycity.runwal.identity.application.service;

import com.mycity.runwal.identity.application.command.RequestOtpCommand;
import com.mycity.runwal.identity.application.command.VerifyOtpCommand;
import com.mycity.runwal.identity.application.port.AccessTokenIssuer;
import com.mycity.runwal.identity.application.port.OtpCodeSource;
import com.mycity.runwal.identity.application.port.OtpHasher;
import com.mycity.runwal.identity.application.port.OtpSender;
import com.mycity.runwal.identity.application.query.OtpSentView;
import com.mycity.runwal.identity.application.query.SessionView;
import com.mycity.runwal.identity.application.query.UserView;
import com.mycity.runwal.identity.domain.exception.OtpRateLimitedException;
import com.mycity.runwal.identity.domain.model.otp.OtpChallenge;
import com.mycity.runwal.identity.domain.model.otp.OtpChallengeRepository;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.AppUserRepository;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** Phone + OTP login. Any valid number can ask for a code (self sign-up); a new number becomes a VERIFIED user. */
@ApplicationService
public class OtpLoginService {

    /** How long a login lasts before the user needs a new code. */
    static final Duration SESSION_VALIDITY = Duration.ofDays(30);

    private final OtpChallengeRepository challenges;
    private final AppUserRepository users;
    private final OtpCodeSource codeSource;
    private final OtpHasher hasher;
    private final OtpSender sender;
    private final AccessTokenIssuer tokenIssuer;
    private final Clock clock;

    public OtpLoginService(OtpChallengeRepository challenges, AppUserRepository users, OtpCodeSource codeSource,
                           OtpHasher hasher, OtpSender sender, AccessTokenIssuer tokenIssuer, Clock clock) {
        this.challenges = challenges;
        this.users = users;
        this.codeSource = codeSource;
        this.hasher = hasher;
        this.sender = sender;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Transactional
    public OtpSentView requestOtp(RequestOtpCommand command) {
        PhoneNumber phone = PhoneNumber.parse(command.phone());
        Instant now = clock.instant();
        String code = codeSource.newCode(phone);
        String hash = hasher.hash(phone, code);
        Optional<OtpChallenge> existing = challenges.findByPhone(phone);
        OtpChallenge challenge = existing.orElseGet(() -> OtpChallenge.issue(phone, hash, now));
        if (existing.isPresent()) {
            try {
                challenge.reissue(hash, now);
            } catch (OtpRateLimitedException e) {
                throw new TooManyCodesException(e.getMessage());
            }
        }
        challenges.save(challenge);
        sender.send(phone, code);
        return new OtpSentView(phone.masked(), OtpChallenge.VALIDITY.toSeconds(),
                OtpChallenge.RESEND_COOLDOWN.toSeconds());
    }

    /** A wrong code still counts as an attempt, so the transaction commits even when the code is rejected. */
    @Transactional(noRollbackFor = LoginFailedException.class)
    public SessionView verify(VerifyOtpCommand command) {
        PhoneNumber phone = PhoneNumber.parse(command.phone());
        Instant now = clock.instant();
        OtpChallenge challenge = challenges.findByPhone(phone).orElseThrow(LoginFailedException::new);
        boolean accepted = challenge.verify(hasher.hash(phone, String.valueOf(command.code()).strip()), now);
        challenges.save(challenge);
        if (!accepted) {
            throw new LoginFailedException();
        }
        AppUser user = users.findByPhone(phone).orElseGet(() -> {
            AppUser created = AppUser.verified(users.nextId(), phone, now);
            users.save(created);
            return created;
        });
        Instant expiresAt = now.plus(SESSION_VALIDITY);
        return new SessionView(tokenIssuer.issue(user, now, expiresAt), expiresAt, UserView.of(user));
    }
}
