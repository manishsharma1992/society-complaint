package com.mycity.runwal.integration;

import com.mycity.runwal.identity.application.port.FlatDirectory;
import com.mycity.runwal.residency.application.query.GetFlatQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import org.springframework.stereotype.Component;

/** Anticorruption layer: answers Identity's "does this flat exist?" (self sign-up) from Residency, in-process. */
@Component
class IdentityFlatDirectory implements FlatDirectory {

    private final ResidencyQueryService residency;

    IdentityFlatDirectory(ResidencyQueryService residency) {
        this.residency = residency;
    }

    @Override
    public boolean exists(SocietyId societyId, FlatId flatId) {
        return residency.flat(new GetFlatQuery(societyId.value(), flatId.value())).isPresent();
    }
}
