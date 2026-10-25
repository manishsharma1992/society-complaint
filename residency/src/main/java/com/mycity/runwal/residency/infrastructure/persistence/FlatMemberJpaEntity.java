package com.mycity.runwal.residency.infrastructure.persistence;

import com.mycity.runwal.residency.domain.model.flat.MembershipType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(schema = "residency", name = "flat_member")
class FlatMemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    MembershipType type;

    @Column(nullable = false)
    Instant since;

    protected FlatMemberJpaEntity() {
    }
}
