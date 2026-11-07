package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import java.time.Instant;
import java.util.Objects;

/**
 * Someone's report that a published post breaks the content policy. Anyone can report, even without logging in.
 *
 * @param reporterKey an opaque, keyed hash that tells reporters apart without identifying them; one report per
 *                    reporter and post
 */
@ValueObject
public record PostReport(String reporterKey, String reason, Instant reportedAt) {

    public static final int MAX_REASON_LENGTH = 500;

    public PostReport {
        if (reporterKey == null || reporterKey.isBlank() || reporterKey.length() > 100) {
            throw new InvalidPostException("A report needs a reporter key of 1 to 100 characters");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidPostException("A report needs a reason");
        }
        reason = reason.strip();
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new InvalidPostException("A report reason can have at most " + MAX_REASON_LENGTH + " characters");
        }
        Objects.requireNonNull(reportedAt, "reportedAt");
    }
}
