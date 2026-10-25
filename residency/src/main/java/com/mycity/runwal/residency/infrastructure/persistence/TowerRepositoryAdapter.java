package com.mycity.runwal.residency.infrastructure.persistence;

import com.mycity.runwal.residency.domain.model.tower.Tower;
import com.mycity.runwal.residency.domain.model.tower.TowerRepository;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class TowerRepositoryAdapter implements TowerRepository {

    private final TowerJpaRepository jpaRepository;

    TowerRepositoryAdapter(TowerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Tower> findBySociety(SocietyId societyId) {
        return jpaRepository.findBySocietyIdOrderById(societyId.value()).stream()
                .map(ResidencyMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Tower> findById(SocietyId societyId, TowerId id) {
        return jpaRepository.findByIdAndSocietyId(id.value(), societyId.value()).map(ResidencyMapper::toDomain);
    }
}
