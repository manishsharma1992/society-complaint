package com.mycity.runwal.residency.application.query;

/** Societies within {@code meters} of this one (itself included) that have this add-on switched on. */
public record NearbySocietiesQuery(long societyId, double meters, String addOn) {
}
