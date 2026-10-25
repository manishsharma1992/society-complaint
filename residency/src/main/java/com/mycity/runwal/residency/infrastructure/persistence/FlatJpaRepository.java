package com.mycity.runwal.residency.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FlatJpaRepository extends JpaRepository<FlatJpaEntity, Long> {

    Optional<FlatJpaEntity> findByIdAndSocietyId(long id, long societyId);

    List<FlatJpaEntity> findBySocietyIdAndTowerIdOrderByFloorAscUnitAsc(long societyId, long towerId);

    @Query("""
            select f from FlatJpaEntity f join f.members m
            where f.societyId = :societyId and m.userId = :userId
            order by f.towerId, f.floor, f.unit""")
    List<FlatJpaEntity> findByMember(@Param("societyId") long societyId, @Param("userId") long userId);
}
