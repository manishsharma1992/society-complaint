package com.mycity.runwal.complaint.infrastructure.persistence;

import com.mycity.runwal.complaint.domain.model.complaint.ComplaintScope;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ComplaintJpaRepository extends JpaRepository<ComplaintJpaEntity, Long> {

    Optional<ComplaintJpaEntity> findByIdAndSocietyId(long id, long societyId);

    @Query("""
            select c from ComplaintJpaEntity c
            where c.societyId = :societyId
              and (:status is null or c.status = :status)
              and (:towerId is null or c.towerId = :towerId)
              and (:assignedTo is null or c.assignedTo = :assignedTo)
            order by c.raisedAt desc, c.id desc""")
    List<ComplaintJpaEntity> search(@Param("societyId") long societyId,
                                    @Param("status") ComplaintStatus status,
                                    @Param("towerId") Long towerId,
                                    @Param("assignedTo") Long assignedTo,
                                    Pageable pageable);

    @Query("""
            select c from ComplaintJpaEntity c
            where c.societyId = :societyId
              and (:status is null or c.status = :status)
              and (:towerId is null or c.towerId = :towerId)
              and (:assignedTo is null or c.assignedTo = :assignedTo)
              and (c.raisedBy = :userId or c.scope = :commonArea or c.flatId in :flatIds)
            order by c.raisedAt desc, c.id desc""")
    List<ComplaintJpaEntity> searchVisibleTo(@Param("societyId") long societyId,
                                             @Param("status") ComplaintStatus status,
                                             @Param("towerId") Long towerId,
                                             @Param("assignedTo") Long assignedTo,
                                             @Param("userId") long userId,
                                             @Param("commonArea") ComplaintScope commonArea,
                                             @Param("flatIds") Collection<Long> flatIds,
                                             Pageable pageable);

    @Query("""
            select c from ComplaintJpaEntity c
            where c.societyId = :societyId
              and c.assignedTo is not null
              and (:towerId is null or c.towerId = :towerId)
              and c.raisedAt >= :from and c.raisedAt < :to
            order by c.raisedAt desc, c.id desc""")
    List<ComplaintJpaEntity> searchAssignedRaisedBetween(@Param("societyId") long societyId,
                                                         @Param("towerId") Long towerId,
                                                         @Param("from") Instant from,
                                                         @Param("to") Instant to,
                                                         Pageable pageable);

    @Query(value = "select nextval('complaint.complaint_id_seq')", nativeQuery = true)
    long nextId();
}
