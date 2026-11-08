package com.mycity.runwal.complaint.infrastructure.persistence;

import com.mycity.runwal.complaint.domain.model.assignment.AutoAssignment;
import com.mycity.runwal.complaint.domain.model.assignment.AutoAssignmentRepository;
import com.mycity.runwal.shared.id.SocietyId;
import org.springframework.stereotype.Component;

@Component
class AutoAssignmentRepositoryAdapter implements AutoAssignmentRepository {

    private final AutoAssignmentJpaRepository jpaRepository;

    AutoAssignmentRepositoryAdapter(AutoAssignmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AutoAssignment find(SocietyId societyId) {
        return jpaRepository.findById(societyId.value())
                .map(row -> new AutoAssignment(societyId, row.enabled))
                .orElseGet(() -> AutoAssignment.offFor(societyId));
    }

    @Override
    public void save(AutoAssignment autoAssignment) {
        jpaRepository.save(new AutoAssignmentJpaEntity(autoAssignment.societyId().value(), autoAssignment.isOn()));
    }
}
