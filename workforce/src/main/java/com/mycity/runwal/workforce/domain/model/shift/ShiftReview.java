package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.time.Instant;
import java.util.Objects;

/**
 * An admin's verdict after comparing the selfies with the staff member and checking where they were.
 *
 * @param reason required when disputed
 */
@ValueObject
public record ShiftReview(Decision decision, UserId reviewer, Instant reviewedAt, String reason) {

    public static final int MAX_REASON_LENGTH = 500;

    public enum Decision {
        CONFIRMED,
        DISPUTED
    }

    public ShiftReview {
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(reviewer, "reviewer");
        Objects.requireNonNull(reviewedAt, "reviewedAt");
        reason = reason == null || reason.isBlank() ? null : reason.strip();
        if (decision == Decision.DISPUTED && reason == null) {
            throw new InvalidShiftException("Disputing a shift needs a reason");
        }
        if (reason != null && reason.length() > MAX_REASON_LENGTH) {
            throw new InvalidShiftException("The reason can be at most " + MAX_REASON_LENGTH + " characters");
        }
    }
}
