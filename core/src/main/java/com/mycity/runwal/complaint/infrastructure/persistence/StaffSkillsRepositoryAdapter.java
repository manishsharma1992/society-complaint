package com.mycity.runwal.complaint.infrastructure.persistence;

import com.mycity.runwal.complaint.domain.model.assignment.StaffSkills;
import com.mycity.runwal.complaint.domain.model.assignment.StaffSkillsRepository;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class StaffSkillsRepositoryAdapter implements StaffSkillsRepository {

    private final StaffSkillsCategoryJpaRepository jpaRepository;

    StaffSkillsRepositoryAdapter(StaffSkillsCategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<StaffSkills> findBySociety(SocietyId societyId) {
        Map<Long, Set<ComplaintCategoryId>> byStaff = jpaRepository.findByIdSocietyId(societyId.value()).stream()
                .collect(Collectors.groupingBy(row -> row.id.staffId, TreeMap::new,
                        Collectors.mapping(row -> new ComplaintCategoryId(row.id.categoryId), Collectors.toSet())));
        return byStaff.entrySet().stream()
                .map(e -> new StaffSkills(societyId, new UserId(e.getKey()), e.getValue()))
                .toList();
    }

    @Override
    public StaffSkills find(SocietyId societyId, UserId staffId) {
        Set<ComplaintCategoryId> categories = jpaRepository.findByIdSocietyIdAndIdStaffId(societyId.value(), staffId.value())
                .stream().map(row -> new ComplaintCategoryId(row.id.categoryId)).collect(Collectors.toSet());
        return new StaffSkills(societyId, staffId, categories);
    }

    /** Replaces the staff member's rows with their current categories. */
    @Override
    public void save(StaffSkills skills) {
        long society = skills.societyId().value();
        long staff = skills.staffId().value();
        jpaRepository.deleteAll(jpaRepository.findByIdSocietyIdAndIdStaffId(society, staff));
        jpaRepository.flush();
        jpaRepository.saveAll(skills.categories().stream()
                .map(c -> new StaffSkillsCategoryJpaEntity(society, staff, c.value())).toList());
    }
}
