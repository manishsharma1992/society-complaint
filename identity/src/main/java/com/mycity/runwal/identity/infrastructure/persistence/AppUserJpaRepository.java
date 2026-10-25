package com.mycity.runwal.identity.infrastructure.persistence;

import com.mycity.runwal.identity.domain.model.user.Role;
import com.mycity.runwal.identity.domain.model.user.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, Long> {

    Optional<AppUserJpaEntity> findByPhone(String phone);

    List<AppUserJpaEntity> findByStatusAndClaimSocietyIdOrderByCreatedAt(UserStatus status, long claimSocietyId);

    @Query("""
            select distinct u from AppUserJpaEntity u join u.roles r
            where r.role = :role and r.societyId = :societyId and u.status = :status
            order by u.name""")
    List<AppUserJpaEntity> findWithRole(@Param("role") Role role, @Param("societyId") long societyId,
                                        @Param("status") UserStatus status);

    @Query(value = "select nextval('identity.app_user_id_seq')", nativeQuery = true)
    long nextId();
}
