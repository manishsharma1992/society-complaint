package com.mycity.runwal.workforce.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shift", schema = WorkforceSchemaConfig.SCHEMA)
class ShiftJpaEntity {

    @Id
    Long id;

    @Column(name = "society_id", nullable = false)
    long societyId;

    @Column(name = "staff_id", nullable = false)
    long staffId;

    @Column(name = "started_at", nullable = false)
    Instant startedAt;

    @Column(name = "ended_at")
    Instant endedAt;
}
