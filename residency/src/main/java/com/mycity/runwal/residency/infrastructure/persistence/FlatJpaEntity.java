package com.mycity.runwal.residency.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(schema = "residency", name = "flat")
class FlatJpaEntity {

    /** Flats are created by migrations (the seed), not by the app. */
    @Id
    Long id;

    @Column(name = "society_id", nullable = false)
    long societyId;

    @Column(name = "tower_id", nullable = false)
    long towerId;

    @Column(nullable = false)
    int floor;

    @Column(nullable = false)
    int unit;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "flat_id", nullable = false)
    @OrderBy("id")
    @BatchSize(size = 100)
    List<FlatMemberJpaEntity> members = new ArrayList<>();

    protected FlatJpaEntity() {
    }
}
