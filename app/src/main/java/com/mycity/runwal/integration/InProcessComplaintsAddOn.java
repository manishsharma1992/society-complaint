package com.mycity.runwal.integration;

import com.mycity.runwal.complaint.application.port.ComplaintsAddOn;
import com.mycity.runwal.residency.application.query.AddOnOnQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.SocietyId;
import org.springframework.stereotype.Component;

/** Answers Complaint Management's "is my add-on on?" from Residency, where each society's add-ons are kept. */
@Component
class InProcessComplaintsAddOn implements ComplaintsAddOn {

    private final ResidencyQueryService residency;

    InProcessComplaintsAddOn(ResidencyQueryService residency) {
        this.residency = residency;
    }

    @Override
    public boolean isOnFor(SocietyId societyId) {
        return residency.addOnOn(new AddOnOnQuery(societyId.value(), "COMPLAINTS"));
    }
}
