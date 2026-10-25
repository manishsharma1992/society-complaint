package com.mycity.runwal.residency.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TowerJpaRepository extends JpaRepository<TowerJpaEntity, Long> {

    List<TowerJpaEntity> findBySocietyIdOrderById(long societyId);

    Optional<TowerJpaEntity> findByIdAndSocietyId(long id, long societyId);
}
