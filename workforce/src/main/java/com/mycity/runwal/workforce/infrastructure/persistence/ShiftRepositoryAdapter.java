package com.mycity.runwal.workforce.infrastructure.persistence;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.ShiftId;
import com.mycity.runwal.workforce.domain.model.shift.ShiftRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
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
        jpaRepository.save(ShiftMapper.toEntity(shift));
    }

    @Override
    public Optional<Shift> findById(SocietyId societyId, ShiftId id) {
        return jpaRepository.findByIdAndSocietyId(id.value(), societyId.value()).map(ShiftMapper::toDomain);
    }

    @Override
    public Optional<Shift> findOpen(SocietyId societyId, UserId staffId) {
        return jpaRepository.findBySocietyIdAndStaffIdAndEndedAtIsNull(societyId.value(), staffId.value())
                .map(ShiftMapper::toDomain);
    }

    @Override
    public List<Shift> findAllOpen(SocietyId societyId) {
        return jpaRepository.findBySocietyIdAndEndedAtIsNull(societyId.value()).stream()
                .map(ShiftMapper::toDomain)
                .toList();
    }

    @Override
    public List<Shift> findOverlapping(SocietyId societyId, Instant from, Instant to) {
        return jpaRepository.findOverlapping(societyId.value(), from, to).stream()
                .map(ShiftMapper::toDomain)
                .toList();
    }

    @Override
    public List<Shift> findHoldingEvidenceStartedBefore(Instant cutoff, int limit) {
        return jpaRepository.findHoldingEvidenceStartedBefore(cutoff, PageRequest.of(0, limit)).stream()
                .map(ShiftMapper::toDomain)
                .toList();
    }
}
