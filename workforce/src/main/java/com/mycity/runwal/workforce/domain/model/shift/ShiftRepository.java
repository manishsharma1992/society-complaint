package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface ShiftRepository {

    ShiftId nextId();

    void save(Shift shift);

    Optional<Shift> findById(SocietyId societyId, ShiftId id);

    /** The staff member's shift that has not ended yet, if any. */
    Optional<Shift> findOpen(SocietyId societyId, UserId staffId);

    /** All open shifts of the society: who is on duty now. */
    List<Shift> findAllOpen(SocietyId societyId);

    /** Shifts of the society that started before {@code to} and had not ended by {@code from}, oldest first. */
    List<Shift> findOverlapping(SocietyId societyId, Instant from, Instant to);

    /** Shifts of any society, started before {@code cutoff}, that still hold selfies or locations; oldest first. */
    List<Shift> findHoldingEvidenceStartedBefore(Instant cutoff, int limit);
}
