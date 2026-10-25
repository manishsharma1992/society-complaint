package com.mycity.runwal.identity.domain.model.otp;

import com.mycity.runwal.identity.domain.exception.OtpRateLimitedException;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * The one-time code most recently sent to a phone number. Only a hash of the code is kept.
 * A code is valid for {@link #VALIDITY}, can be used once, and allows {@link #MAX_ATTEMPTS} wrong tries.
 * Sending is limited per number to protect people from SMS flooding and the society from SMS costs.
 */
@AggregateRoot
public class OtpChallenge {

    public static final Duration VALIDITY = Duration.ofMinutes(5);
    public static final int MAX_ATTEMPTS = 5;
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    public static final int MAX_SENDS_PER_HOUR = 5;

    private final PhoneNumber phone;
    private String codeHash;
    private Instant expiresAt;
    private int failedAttempts;
    private boolean used;
    private Instant lastSentAt;
    private Instant windowStart;
    private int sendsInWindow;

    public OtpChallenge(PhoneNumber phone, String codeHash, Instant expiresAt, int failedAttempts, boolean used,
                        Instant lastSentAt, Instant windowStart, int sendsInWindow) {
        this.phone = Objects.requireNonNull(phone, "phone");
        this.codeHash = Objects.requireNonNull(codeHash, "codeHash");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.failedAttempts = failedAttempts;
        this.used = used;
        this.lastSentAt = Objects.requireNonNull(lastSentAt, "lastSentAt");
        this.windowStart = Objects.requireNonNull(windowStart, "windowStart");
        this.sendsInWindow = sendsInWindow;
    }

    /** The first code for this number. */
    public static OtpChallenge issue(PhoneNumber phone, String codeHash, Instant now) {
        return new OtpChallenge(phone, codeHash, now.plus(VALIDITY), 0, false, now, now, 1);
    }

    /** A new code replaces the previous one, within the sending limits. */
    public void reissue(String newCodeHash, Instant now) {
        if (now.isBefore(lastSentAt.plus(RESEND_COOLDOWN))) {
            long wait = Duration.between(now, lastSentAt.plus(RESEND_COOLDOWN)).toSeconds() + 1;
            throw new OtpRateLimitedException("Please wait " + wait + " seconds before requesting another code");
        }
        if (!now.isBefore(windowStart.plus(Duration.ofHours(1)))) {
            windowStart = now;
            sendsInWindow = 0;
        }
        if (sendsInWindow >= MAX_SENDS_PER_HOUR) {
            throw new OtpRateLimitedException("Too many codes requested for this number; please try again in an hour");
        }
        codeHash = Objects.requireNonNull(newCodeHash, "newCodeHash");
        expiresAt = now.plus(VALIDITY);
        failedAttempts = 0;
        used = false;
        lastSentAt = now;
        sendsInWindow++;
    }

    /**
     * Checks a code (as its hash). A wrong code counts as an attempt, so the caller must save the challenge
     * whatever the result. A correct code can be used only once.
     */
    public boolean verify(String candidateHash, Instant now) {
        if (used || !now.isBefore(expiresAt) || failedAttempts >= MAX_ATTEMPTS) {
            return false;
        }
        boolean matches = MessageDigest.isEqual(codeHash.getBytes(StandardCharsets.UTF_8),
                String.valueOf(candidateHash).getBytes(StandardCharsets.UTF_8));
        if (matches) {
            used = true;
        } else {
            failedAttempts++;
        }
        return matches;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public String codeHash() {
        return codeHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public int failedAttempts() {
        return failedAttempts;
    }

    public boolean isUsed() {
        return used;
    }

    public Instant lastSentAt() {
        return lastSentAt;
    }

    public Instant windowStart() {
        return windowStart;
    }

    public int sendsInWindow() {
        return sendsInWindow;
    }
}
