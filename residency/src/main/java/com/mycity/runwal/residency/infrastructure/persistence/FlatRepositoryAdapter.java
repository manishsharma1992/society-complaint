package com.mycity.runwal.residency.infrastructure.persistence;

import com.mycity.runwal.residency.domain.model.flat.Flat;
import com.mycity.runwal.residency.domain.model.flat.FlatRepository;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class FlatRepositoryAdapter implements FlatRepository {

    private final FlatJpaRepository jpaRepository;

    FlatRepositoryAdapter(FlatJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** Flats themselves come from migrations; saving a flat stores its members. */
    @Override
    public void save(Flat flat) {
        FlatJpaEntity entity = jpaRepository.findById(flat.id().value())
                .orElseThrow(() -> new IllegalStateException("Flat " + flat.id().value() + " is not stored"));
        ResidencyMapper.updateMembers(flat, entity);
    }

    @Override
    public Optional<Flat> findById(SocietyId societyId, FlatId id) {
        return jpaRepository.findByIdAndSocietyId(id.value(), societyId.value()).map(ResidencyMapper::toDomain);
    }

    @Override
    public List<Flat> findByTower(SocietyId societyId, TowerId towerId) {
        return jpaRepository.findBySocietyIdAndTowerIdOrderByFloorAscUnitAsc(societyId.value(), towerId.value())
                .stream()
                .map(ResidencyMapper::toDomain)
                .toList();
    }

    @Override
    public List<Flat> findByMember(SocietyId societyId, UserId userId) {
        return jpaRepository.findByMember(societyId.value(), userId.value()).stream()
                .map(ResidencyMapper::toDomain)
                .toList();
    }
}
