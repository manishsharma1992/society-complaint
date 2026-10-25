package com.mycity.runwal.complaint.infrastructure.persistence;

import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryRepository;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ComplaintCategoryRepositoryAdapter implements ComplaintCategoryRepository {

    private final ComplaintCategoryJpaRepository jpaRepository;

    ComplaintCategoryRepositoryAdapter(ComplaintCategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ComplaintCategory> findById(SocietyId societyId, ComplaintCategoryId id) {
        return jpaRepository.findByIdAndSocietyId(id.value(), societyId.value())
                .map(ComplaintCategoryMapper::toDomain);
    }

    @Override
    public List<ComplaintCategory> findActiveBySociety(SocietyId societyId) {
        return jpaRepository.findBySocietyIdAndActiveTrueOrderById(societyId.value()).stream()
                .map(ComplaintCategoryMapper::toDomain)
                .toList();
    }
}
