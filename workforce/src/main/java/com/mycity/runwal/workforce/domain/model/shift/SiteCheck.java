package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.util.Objects;

/**
 * The result of checking a clock-in or clock-out against the site.
 *
 * @param distanceMeters from the site's centre, rounded; null without a location
 */
@ValueObject
public record SiteCheck(Placement placement, Long distanceMeters) {

    public SiteCheck {
        Objects.requireNonNull(placement, "placement");
        if ((placement == Placement.NO_LOCATION) != (distanceMeters == null)) {
            throw new InvalidShiftException("A distance exists exactly when there is a location");
        }
    }

    public static SiteCheck noLocation() {
        return new SiteCheck(Placement.NO_LOCATION, null);
    }

    /** Anything but "inside" needs an admin's attention. */
    public boolean isInside() {
        return placement == Placement.INSIDE;
    }
}
