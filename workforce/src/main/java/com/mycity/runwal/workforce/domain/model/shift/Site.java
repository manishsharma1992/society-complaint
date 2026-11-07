package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.util.Objects;

/** The society's grounds, as a circle: staff are expected to clock in and out inside it. */
@ValueObject
public record Site(GeoPoint centre, double radiusMeters) {

    public Site {
        Objects.requireNonNull(centre, "centre");
        if (!(radiusMeters > 0)) {
            throw new InvalidShiftException("A site needs a positive radius, but was " + radiusMeters);
        }
    }

    /** Inside when the reported point is within the radius; the phone's accuracy is shown, not used to excuse. */
    public SiteCheck check(Location location) {
        if (location == null) {
            return SiteCheck.noLocation();
        }
        double meters = centre.metersTo(location.point());
        return new SiteCheck(meters <= radiusMeters ? Placement.INSIDE : Placement.OUTSIDE, Math.round(meters));
    }
}
