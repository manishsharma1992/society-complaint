package com.mycity.runwal.identity.application.query;

import java.util.List;

/** Names for these user IDs (as shown on complaints and comments), at most {@link #MAX_IDS} at a time. */
public record UserNamesQuery(long societyId, List<Long> userIds) {

    public static final int MAX_IDS = 100;
}
