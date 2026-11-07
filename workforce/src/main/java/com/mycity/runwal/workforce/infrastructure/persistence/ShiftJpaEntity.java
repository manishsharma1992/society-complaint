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

    @Column(name = "start_selfie_key", length = 200)
    String startSelfieKey;

    @Column(name = "start_latitude")
    Double startLatitude;

    @Column(name = "start_longitude")
    Double startLongitude;

    @Column(name = "start_accuracy_m")
    Double startAccuracyMeters;

    @Column(name = "start_placement", length = 20)
    String startPlacement;

    @Column(name = "start_distance_m")
    Long startDistanceMeters;

    @Column(name = "end_selfie_key", length = 200)
    String endSelfieKey;

    @Column(name = "end_latitude")
    Double endLatitude;

    @Column(name = "end_longitude")
    Double endLongitude;

    @Column(name = "end_accuracy_m")
    Double endAccuracyMeters;

    @Column(name = "end_placement", length = 20)
    String endPlacement;

    @Column(name = "end_distance_m")
    Long endDistanceMeters;

    @Column(name = "review_decision", length = 20)
    String reviewDecision;

    @Column(name = "reviewer_id")
    Long reviewerId;

    @Column(name = "reviewed_at")
    Instant reviewedAt;

    @Column(name = "review_reason", length = 500)
    String reviewReason;
}
