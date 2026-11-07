package com.mycity.runwal.residency.domain.model.society;

import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface SocietyRepository {

    Optional<Society> findById(SocietyId id);

    /** Every society on the platform (a few hundred at most), for finding neighbours. */
    List<Society> findAll();

    void save(Society society);
}
