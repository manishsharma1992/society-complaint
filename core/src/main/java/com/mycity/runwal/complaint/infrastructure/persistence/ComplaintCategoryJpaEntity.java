package com.mycity.runwal.complaint.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "complaint", name = "complaint_category")
class ComplaintCategoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "society_id", nullable = false)
    long societyId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(name = "sla_hours", nullable = false)
    int slaHours;

    @Column(nullable = false)
    boolean active;

    protected ComplaintCategoryJpaEntity() {
    }
}
