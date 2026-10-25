package com.mycity.runwal.residency.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "residency", name = "society")
class SocietyJpaEntity {

    @Id
    Long id;

    @Column(nullable = false, length = 150)
    String name;

    protected SocietyJpaEntity() {
    }
}
