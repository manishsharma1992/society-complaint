package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.util.Objects;

/**
 * Where the phone said it was when the staff member clocked in or out.
 *
 * @param accuracyMeters the phone's own estimate of its error (68% confidence radius)
 */
@ValueObject
public record Location(GeoPoint point, double accuracyMeters) {

    public Location {
        Objects.requireNonNull(point, "point");
        if (!(accuracyMeters >= 0)) {
            throw new InvalidShiftException("Location accuracy must be zero or more metres, but was " + accuracyMeters);
        }
    }
}
