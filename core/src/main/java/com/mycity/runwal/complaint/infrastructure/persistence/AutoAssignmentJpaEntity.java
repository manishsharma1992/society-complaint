package com.mycity.runwal.complaint.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "complaint", name = "auto_assignment")
class AutoAssignmentJpaEntity {

    @Id
    @Column(name = "society_id")
    Long societyId;

    @Column(nullable = false)
    boolean enabled;

    protected AutoAssignmentJpaEntity() {
    }

    AutoAssignmentJpaEntity(long societyId, boolean enabled) {
        this.societyId = societyId;
        this.enabled = enabled;
    }
}
