package com.mycity.runwal.residency.application.query;

/**
 * @param addOn     FEED or COMPLAINTS
 * @param available the platform owner lets the society use it
 * @param on        the society has switched it on
 */
public record AddOnView(String addOn, boolean available, boolean on) {
}
