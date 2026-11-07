package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** A time range [from, to) to report shifts for; at most a little over a year, for the annual review. */
@ValueObject
public record Period(Instant from, Instant to) {

    public static final Duration MAX_LENGTH = Duration.ofDays(400);

    public Period {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!to.isAfter(from)) {
            throw new InvalidShiftException("The period must end after it starts");
        }
        if (Duration.between(from, to).compareTo(MAX_LENGTH) > 0) {
            throw new InvalidShiftException("The period can be at most " + MAX_LENGTH.toDays() + " days long");
        }
    }
}
