package com.mycity.runwal.feed.application.port;

import com.mycity.runwal.shared.id.SocietyId;

/** Whether the society has the Feed add-on switched on (decided in the Residency context). */
public interface FeedAddOn {

    boolean isOnFor(SocietyId societyId);
}
