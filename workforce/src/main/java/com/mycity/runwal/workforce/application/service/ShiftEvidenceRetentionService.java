package com.mycity.runwal.workforce.application.service;

import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.workforce.application.port.SelfieStorage;
import com.mycity.runwal.workforce.domain.model.shift.ClockEvidence;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.ShiftRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes selfies and locations once {@link Shift#EVIDENCE_KEPT_FOR} has passed (DPDP: keep personal data only
 * as long as needed). Shift times, site checks and reviews stay for the annual review. Runs as the system, daily.
 */
@ApplicationService
public class ShiftEvidenceRetentionService {

    static final int BATCH = 100;

    private final ShiftRepository shifts;
    private final SelfieStorage selfies;
    private final Clock clock;

    public ShiftEvidenceRetentionService(ShiftRepository shifts, SelfieStorage selfies, Clock clock) {
        this.shifts = shifts;
        this.selfies = selfies;
        this.clock = clock;
    }

    /** Forgets one batch of expired evidence; returns how many shifts were cleaned (call again while it is BATCH). */
    @Transactional
    public int forgetExpiredBatch() {
        Instant cutoff = clock.instant().minus(Shift.EVIDENCE_KEPT_FOR);
        List<Shift> expired = shifts.findHoldingEvidenceStartedBefore(cutoff, BATCH);
        for (Shift shift : expired) {
            Stream.of(shift.startEvidence(), shift.endEvidence())
                    .flatMap(Optional::stream)
                    .map(ClockEvidence::selfieKey)
                    .flatMap(Optional::stream)
                    .forEach(key -> selfies.delete(key.value()));
            shift.forgetEvidence();
            shifts.save(shift);
        }
        return expired.size();
    }
}
