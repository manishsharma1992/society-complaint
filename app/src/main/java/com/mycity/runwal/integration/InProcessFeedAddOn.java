package com.mycity.runwal.integration;

import com.mycity.runwal.feed.application.port.FeedAddOn;
import com.mycity.runwal.residency.application.query.AddOnOnQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.SocietyId;
import org.springframework.stereotype.Component;

/** Answers Public Feed's "is my add-on on?" from Residency, where each society's add-ons are kept. */
@Component
class InProcessFeedAddOn implements FeedAddOn {

    private final ResidencyQueryService residency;

    InProcessFeedAddOn(ResidencyQueryService residency) {
        this.residency = residency;
    }

    @Override
    public boolean isOnFor(SocietyId societyId) {
        return residency.addOnOn(new AddOnOnQuery(societyId.value(), "FEED"));
    }
}
