package com.mycity.runwal.residency.infrastructure.persistence;

import com.mycity.runwal.residency.domain.model.society.Society;
import com.mycity.runwal.residency.domain.model.society.SocietyRepository;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;
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

    @Override
    public List<Society> findAll() {
        return jpaRepository.findAll().stream().map(ResidencyMapper::toDomain).toList();
    }

    /** Only the add-ons change after a society is created; name and location are set in the database. */
    @Override
    public void save(Society society) {
        SocietyJpaEntity entity = jpaRepository.findById(society.id().value())
                .orElseThrow(() -> new IllegalStateException("Society " + society.id().value() + " does not exist"));
        ResidencyMapper.copyAddOns(society, entity);
    }
}
