package com.mycity.runwal.complaint.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(schema = "complaint", name = "complaint_photo")
class ComplaintPhotoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** 1-based order in which the photo was added. */
    @Column(nullable = false)
    int position;

    @Column(name = "storage_key", nullable = false, length = 300)
    String storageKey;

    @Column(name = "content_type", nullable = false, length = 50)
    String contentType;

    @Column(name = "size_bytes", nullable = false)
    long sizeBytes;

    @Column(name = "uploaded_by", nullable = false)
    long uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    Instant uploadedAt;

    protected ComplaintPhotoJpaEntity() {
    }
}
