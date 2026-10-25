package com.mycity.runwal.complaint.infrastructure.persistence;

import com.mycity.runwal.complaint.domain.model.complaint.ComplaintScope;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(schema = "complaint", name = "complaint")
class ComplaintJpaEntity {

    /** Assigned from the complaint_id_seq sequence by {@link ComplaintRepositoryAdapter#nextId()}. */
    @Id
    Long id;

    @Column(name = "society_id", nullable = false)
    long societyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ComplaintScope scope;

    @Column(name = "tower_id")
    Long towerId;

    @Column(name = "flat_id")
    Long flatId;

    @Column(name = "category_id", nullable = false)
    long categoryId;

    @Column(nullable = false, length = 150)
    String title;

    @Column(length = 2000)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ComplaintStatus status;

    @Column(name = "raised_by", nullable = false)
    long raisedBy;

    @Column(name = "assigned_to")
    Long assignedTo;

    @Column(name = "raised_at", nullable = false)
    Instant raisedAt;

    @Column(name = "due_at", nullable = false)
    Instant dueAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "complaint_id", nullable = false)
    @OrderBy("sequenceNo")
    @BatchSize(size = 50)
    List<ComplaintStatusChangeJpaEntity> history = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "complaint_id", nullable = false)
    @OrderBy("position")
    @BatchSize(size = 50)
    List<ComplaintPhotoJpaEntity> photos = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "complaint_id", nullable = false)
    @OrderBy("sequenceNo")
    @BatchSize(size = 50)
    List<ComplaintCommentJpaEntity> comments = new ArrayList<>();

    protected ComplaintJpaEntity() {
    }
}
