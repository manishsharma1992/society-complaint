package com.mycity.runwal.workforce.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShiftJpaRepository extends JpaRepository<ShiftJpaEntity, Long> {

    Optional<ShiftJpaEntity> findByIdAndSocietyId(long id, long societyId);

    Optional<ShiftJpaEntity> findBySocietyIdAndStaffIdAndEndedAtIsNull(long societyId, long staffId);

    List<ShiftJpaEntity> findBySocietyIdAndEndedAtIsNull(long societyId);

    @Query("""
            select s from ShiftJpaEntity s
            where s.societyId = :societyId and s.startedAt < :to and (s.endedAt is null or s.endedAt > :from)
            order by s.startedAt asc, s.id asc""")
    List<ShiftJpaEntity> findOverlapping(@Param("societyId") long societyId, @Param("from") Instant from,
                                         @Param("to") Instant to);

    @Query("""
            select s from ShiftJpaEntity s
            where s.startedAt < :cutoff
              and (s.startSelfieKey is not null or s.endSelfieKey is not null
                   or s.startLatitude is not null or s.endLatitude is not null)
            order by s.startedAt asc, s.id asc""")
    List<ShiftJpaEntity> findHoldingEvidenceStartedBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query(value = "select nextval('workforce.shift_id_seq')", nativeQuery = true)
    long nextId();
}
