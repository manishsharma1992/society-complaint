package com.mycity.runwal.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(schema = "identity", name = "otp_challenge")
class OtpChallengeJpaEntity {

    @Id
    @Column(length = 13)
    String phone;

    @Column(name = "code_hash", nullable = false, length = 100)
    String codeHash;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "failed_attempts", nullable = false)
    int failedAttempts;

    @Column(nullable = false)
    boolean used;

    @Column(name = "last_sent_at", nullable = false)
    Instant lastSentAt;

    @Column(name = "window_start", nullable = false)
    Instant windowStart;

    @Column(name = "sends_in_window", nullable = false)
    int sendsInWindow;

    protected OtpChallengeJpaEntity() {
    }
}
