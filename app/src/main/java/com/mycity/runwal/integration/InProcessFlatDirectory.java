package com.mycity.runwal.integration;

import com.mycity.runwal.complaint.application.port.FlatDirectory;
import com.mycity.runwal.residency.application.query.FlatsOfMemberQuery;
import com.mycity.runwal.residency.application.query.GetFlatQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Anticorruption layer between Complaint Management and Residency while both run in this app: answers the complaint
 * context's {@link FlatDirectory} port from Residency's published query service, translating its read model into
 * the complaint context's own types. Neither context knows the other; only this glue module sees both.
 * When Residency becomes its own service, delete this class and set residency.base-url (ResidencyClient takes over).
 */
@Component
class InProcessFlatDirectory implements FlatDirectory {

    private final ResidencyQueryService residency;

    InProcessFlatDirectory(ResidencyQueryService residency) {
        this.residency = residency;
    }

    @Override
    public Optional<TowerId> towerOf(SocietyId societyId, FlatId flatId) {
        return residency.flat(new GetFlatQuery(societyId.value(), flatId.value()))
                .map(flat -> new TowerId(flat.towerId()));
    }

    /** Asked for the logged-in user themself, which Residency allows. */
    @Override
    public Set<FlatId> flatsOf(SocietyId societyId, UserId userId) {
        return residency.flatsOfMember(new FlatsOfMemberQuery(societyId.value(), userId.value())).stream()
                .map(flat -> new FlatId(flat.id()))
                .collect(Collectors.toSet());
    }
}
