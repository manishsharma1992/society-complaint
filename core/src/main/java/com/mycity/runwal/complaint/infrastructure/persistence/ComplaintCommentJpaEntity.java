package com.mycity.runwal.complaint.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(schema = "complaint", name = "complaint_comment")
class ComplaintCommentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "sequence_no", nullable = false)
    int sequenceNo;

    @Column(name = "author_id", nullable = false)
    long authorId;

    @Column(nullable = false, length = 1000)
    String text;

    @Column(name = "posted_at", nullable = false)
    Instant postedAt;

    protected ComplaintCommentJpaEntity() {
    }
}
