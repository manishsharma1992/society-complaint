package com.mycity.runwal.complaint.infrastructure.persistence;

import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
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
@Table(schema = "complaint", name = "complaint_status_history")
class ComplaintStatusChangeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "sequence_no", nullable = false)
    int sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    ComplaintStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    ComplaintStatus toStatus;

    @Column(name = "changed_by", nullable = false)
    long changedBy;

    @Column(name = "changed_at", nullable = false)
    Instant changedAt;

    @Column(length = 500)
    String note;

    protected ComplaintStatusChangeJpaEntity() {
    }
}
