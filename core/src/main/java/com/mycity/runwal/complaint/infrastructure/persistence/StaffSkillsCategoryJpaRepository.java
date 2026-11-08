package com.mycity.runwal.complaint.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface StaffSkillsCategoryJpaRepository extends JpaRepository<StaffSkillsCategoryJpaEntity, StaffSkillsCategoryJpaEntity.Key> {

    List<StaffSkillsCategoryJpaEntity> findByIdSocietyId(long societyId);

    List<StaffSkillsCategoryJpaEntity> findByIdSocietyIdAndIdStaffId(long societyId, long staffId);
}
