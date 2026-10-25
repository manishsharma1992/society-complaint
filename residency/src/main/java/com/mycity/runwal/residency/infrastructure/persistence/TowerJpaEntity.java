package com.mycity.runwal.residency.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "residency", name = "tower")
class TowerJpaEntity {

    @Id
    Long id;

    @Column(name = "society_id", nullable = false)
    long societyId;

    @Column(nullable = false, length = 10)
    String code;

    @Column(nullable = false, length = 100)
    String name;

    @Column(name = "residential_floors", nullable = false)
    int residentialFloors;

    protected TowerJpaEntity() {
    }
}
