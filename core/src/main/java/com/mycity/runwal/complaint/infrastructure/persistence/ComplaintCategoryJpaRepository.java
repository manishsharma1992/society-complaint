package com.mycity.runwal.complaint.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ComplaintCategoryJpaRepository extends JpaRepository<ComplaintCategoryJpaEntity, Long> {

    Optional<ComplaintCategoryJpaEntity> findByIdAndSocietyId(long id, long societyId);

    List<ComplaintCategoryJpaEntity> findBySocietyIdAndActiveTrueOrderById(long societyId);
}
