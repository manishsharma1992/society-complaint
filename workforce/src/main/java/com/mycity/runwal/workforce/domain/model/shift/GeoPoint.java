package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;

/** A place on Earth in degrees (WGS 84, as phones report it). */
@ValueObject
public record GeoPoint(double latitude, double longitude) {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public GeoPoint {
        if (!(latitude >= -90 && latitude <= 90) || !(longitude >= -180 && longitude <= 180)) {
            throw new InvalidShiftException("Not a valid location: " + latitude + ", " + longitude);
        }
    }

    /** Great-circle distance (haversine); accurate to well under a metre at society scale. */
    public double metersTo(GeoPoint other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLng = Math.toRadians(other.longitude - longitude);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(latitude))
                * Math.cos(Math.toRadians(other.latitude)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
