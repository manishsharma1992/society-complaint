package com.mycity.runwal.workforce.infrastructure.persistence;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.ShiftId;
import com.mycity.runwal.workforce.domain.model.shift.ShiftRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class ShiftRepositoryAdapter implements ShiftRepository {

    private final ShiftJpaRepository jpaRepository;

    ShiftRepositoryAdapter(ShiftJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ShiftId nextId() {
        return new ShiftId(jpaRepository.nextId());
    }

    @Override
    public void save(Shift shift) {
        jpaRepository.save(toEntity(shift));
    }

    @Override
    public Optional<Shift> findOpen(SocietyId societyId, UserId staffId) {
        return jpaRepository.findBySocietyIdAndStaffIdAndEndedAtIsNull(societyId.value(), staffId.value())
                .map(ShiftRepositoryAdapter::toDomain);
    }

    @Override
    public List<Shift> findAllOpen(SocietyId societyId) {
        return jpaRepository.findBySocietyIdAndEndedAtIsNull(societyId.value()).stream()
                .map(ShiftRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Shift> findOverlapping(SocietyId societyId, Instant from, Instant to) {
        return jpaRepository.findOverlapping(societyId.value(), from, to).stream()
                .map(ShiftRepositoryAdapter::toDomain)
                .toList();
    }

    private static ShiftJpaEntity toEntity(Shift shift) {
        ShiftJpaEntity entity = new ShiftJpaEntity();
        entity.id = shift.id().value();
        entity.societyId = shift.societyId().value();
        entity.staffId = shift.staffId().value();
        entity.startedAt = shift.startedAt();
        entity.endedAt = shift.endedAt().orElse(null);
        return entity;
    }

    private static Shift toDomain(ShiftJpaEntity entity) {
        return Shift.reconstitute(new ShiftId(entity.id), new SocietyId(entity.societyId),
                new UserId(entity.staffId), entity.startedAt, entity.endedAt);
    }
}
