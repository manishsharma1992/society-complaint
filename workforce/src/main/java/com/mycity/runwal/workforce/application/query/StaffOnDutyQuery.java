package com.mycity.runwal.workforce.application.query;

/** Who is on shift right now in the society: other contexts ask, e.g. to assign complaints to free staff. */
public record StaffOnDutyQuery(long societyId) {
}
