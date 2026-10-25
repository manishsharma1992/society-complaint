package com.mycity.runwal.residency.domain.model.tower;

import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface TowerRepository {

    /** All towers of the society, in their display order. */
    List<Tower> findBySociety(SocietyId societyId);

    Optional<Tower> findById(SocietyId societyId, TowerId id);
}
