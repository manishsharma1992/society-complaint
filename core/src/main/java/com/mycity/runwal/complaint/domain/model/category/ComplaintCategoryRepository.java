package com.mycity.runwal.complaint.domain.model.category;

import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface ComplaintCategoryRepository {

    Optional<ComplaintCategory> findById(SocietyId societyId, ComplaintCategoryId id);

    /** The categories residents can choose from today, in the society's order. */
    List<ComplaintCategory> findActiveBySociety(SocietyId societyId);
}
