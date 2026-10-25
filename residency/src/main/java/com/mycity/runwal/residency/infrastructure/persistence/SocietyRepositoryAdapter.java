package com.mycity.runwal.residency.infrastructure.persistence;

import com.mycity.runwal.residency.domain.model.society.Society;
import com.mycity.runwal.residency.domain.model.society.SocietyRepository;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class SocietyRepositoryAdapter implements SocietyRepository {

    private final SocietyJpaRepository jpaRepository;

    SocietyRepositoryAdapter(SocietyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Society> findById(SocietyId id) {
        return jpaRepository.findById(id.value()).map(ResidencyMapper::toDomain);
    }
}
