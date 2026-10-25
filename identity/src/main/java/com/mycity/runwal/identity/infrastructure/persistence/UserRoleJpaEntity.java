package com.mycity.runwal.identity.infrastructure.persistence;

import com.mycity.runwal.identity.domain.model.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "identity", name = "user_role")
class UserRoleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    Role role;

    @Column(name = "society_id", nullable = false)
    long societyId;

    @Column(name = "tower_id")
    Long towerId;

    protected UserRoleJpaEntity() {
    }
}
