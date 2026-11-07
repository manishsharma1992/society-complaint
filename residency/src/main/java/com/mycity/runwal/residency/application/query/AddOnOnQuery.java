package com.mycity.runwal.residency.application.query;

/** Whether the society has this add-on (FEED or COMPLAINTS) switched on: other contexts ask before doing anything. */
public record AddOnOnQuery(long societyId, String addOn) {
}
