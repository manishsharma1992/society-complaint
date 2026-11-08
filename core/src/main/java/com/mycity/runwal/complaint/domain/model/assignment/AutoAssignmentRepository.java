package com.mycity.runwal.complaint.domain.model.assignment;

import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import com.mycity.runwal.shared.id.SocietyId;

@DomainRepository
public interface AutoAssignmentRepository {

    /** The society's choice; off when it never chose. */
    AutoAssignment find(SocietyId societyId);

    void save(AutoAssignment autoAssignment);
}
