package com.mycity.runwal.complaint.infrastructure.persistence;

import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Duration;

final class ComplaintCategoryMapper {

    private ComplaintCategoryMapper() {
    }

    static ComplaintCategory toDomain(ComplaintCategoryJpaEntity entity) {
        return new ComplaintCategory(new ComplaintCategoryId(entity.id), new SocietyId(entity.societyId),
                entity.name, Duration.ofHours(entity.slaHours), entity.active);
    }
}
