package com.mycity.runwal.integration;

import com.mycity.runwal.workforce.application.port.ComplaintsAddOn;
import com.mycity.runwal.residency.application.query.AddOnOnQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.SocietyId;
import org.springframework.stereotype.Component;

/** Answers Workforce (staff shifts come with Complaints)'s "is my add-on on?" from Residency, where each society's add-ons are kept. */
@Component
class InProcessWorkforceAddOn implements ComplaintsAddOn {

    private final ResidencyQueryService residency;

    InProcessWorkforceAddOn(ResidencyQueryService residency) {
        this.residency = residency;
    }

    @Override
    public boolean isOnFor(SocietyId societyId) {
        return residency.addOnOn(new AddOnOnQuery(societyId.value(), "COMPLAINTS"));
    }
}
