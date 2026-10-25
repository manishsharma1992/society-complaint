package com.mycity.runwal.residency.domain.model.flat;

import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface FlatRepository {

    void save(Flat flat);

    Optional<Flat> findById(SocietyId societyId, FlatId id);

    /** The flats of one tower, by floor and unit. */
    List<Flat> findByTower(SocietyId societyId, TowerId towerId);

    /** The flats this person is linked to in the society. */
    List<Flat> findByMember(SocietyId societyId, UserId userId);
}
