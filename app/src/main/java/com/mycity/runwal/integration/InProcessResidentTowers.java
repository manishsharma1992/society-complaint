package com.mycity.runwal.integration;

import com.mycity.runwal.feed.application.port.ResidentTowers;
import com.mycity.runwal.feed.domain.model.post.ShownTower;
import com.mycity.runwal.residency.application.query.FlatsOfMemberQuery;
import com.mycity.runwal.residency.application.query.ListTowersQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Anticorruption layer between the Public Feed and Residency while both run in this app: answers the feed's
 * {@link ResidentTowers} port from Residency's published query service, translating a tower into the label shown
 * on a post (e.g. "B2 Radiant").
 */
@Component
class InProcessResidentTowers implements ResidentTowers {

    private final ResidencyQueryService residency;

    InProcessResidentTowers(ResidencyQueryService residency) {
        this.residency = residency;
    }

    /** Asked for the logged-in user themself, which Residency allows. */
    @Override
    public Optional<ShownTower> towerOf(SocietyId societyId, UserId resident, TowerId towerId) {
        boolean livesThere = residency.flatsOfMember(new FlatsOfMemberQuery(societyId.value(), resident.value()))
                .stream()
                .anyMatch(flat -> flat.towerId() == towerId.value());
        if (!livesThere) {
            return Optional.empty();
        }
        return residency.towers(new ListTowersQuery(societyId.value())).stream()
                .filter(tower -> tower.id() == towerId.value())
                .findFirst()
                .map(tower -> new ShownTower(towerId, tower.code() + " " + tower.name()));
    }
}
