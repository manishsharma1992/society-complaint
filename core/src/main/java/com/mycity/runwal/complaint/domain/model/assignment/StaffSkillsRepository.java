package com.mycity.runwal.complaint.domain.model.assignment;

import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;

@DomainRepository
public interface StaffSkillsRepository {

    /** Every staff member of the society who handles at least one category. */
    List<StaffSkills> findBySociety(SocietyId societyId);

    /** What this staff member handles; nothing when never set. */
    StaffSkills find(SocietyId societyId, UserId staffId);

    void save(StaffSkills skills);
}
