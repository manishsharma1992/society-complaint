package com.mycity.runwal.identity.infrastructure.persistence;

import com.mycity.runwal.identity.domain.model.user.Occupancy;
import com.mycity.runwal.identity.domain.model.user.UserStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(schema = "identity", name = "app_user")
class AppUserJpaEntity {

    /** Assigned from app_user_id_seq by {@link AppUserRepositoryAdapter#nextId()}. */
    @Id
    Long id;

    @Column(nullable = false, length = 13)
    String phone;

    @Column(length = 100)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    UserStatus status;

    @Column(name = "claim_society_id")
    Long claimSocietyId;

    @Column(name = "claim_flat_id")
    Long claimFlatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_occupancy", length = 10)
    Occupancy claimOccupancy;

    @Column(name = "rejection_reason", length = 500)
    String rejectionReason;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", nullable = false)
    @OrderBy("id")
    @BatchSize(size = 50)
    List<UserRoleJpaEntity> roles = new ArrayList<>();

    protected AppUserJpaEntity() {
    }
}
