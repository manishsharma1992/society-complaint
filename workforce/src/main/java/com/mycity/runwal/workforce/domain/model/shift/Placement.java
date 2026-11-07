package com.mycity.runwal.workforce.domain.model.shift;

/** Where a clock-in or clock-out happened, relative to the society's site. */
public enum Placement {
    /** Within the site's radius. */
    INSIDE,
    /** Further from the site's centre than its radius. */
    OUTSIDE,
    /** The phone gave no location (permission refused, no GPS signal). */
    NO_LOCATION
}
