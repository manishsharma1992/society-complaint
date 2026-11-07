package com.mycity.runwal.workforce.application.service;

import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.workforce.application.command.EndShiftCommand;
import com.mycity.runwal.workforce.application.command.StartShiftCommand;
import com.mycity.runwal.workforce.application.port.CurrentActor;
import com.mycity.runwal.workforce.application.port.DomainEventPublisher;
import com.mycity.runwal.workforce.application.query.ShiftView;
import com.mycity.runwal.workforce.domain.model.actor.Actor;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.ShiftRepository;
import com.mycity.runwal.workforce.domain.service.WorkforceAccessPolicy;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

/** Staff clock in and out. */
@ApplicationService
public class ShiftService {

    private final ShiftRepository shifts;
    private final CurrentActor currentActor;
    private final WorkforceAccessPolicy accessPolicy;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public ShiftService(ShiftRepository shifts, CurrentActor currentActor, WorkforceAccessPolicy accessPolicy,
                        DomainEventPublisher eventPublisher, Clock clock) {
        this.shifts = shifts;
        this.currentActor = currentActor;
        this.accessPolicy = accessPolicy;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ShiftView start(StartShiftCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        Actor actor = staffOf(societyId);
        if (shifts.findOpen(societyId, actor.userId()).isPresent()) {
            throw new ShiftAlreadyOpenException();
        }
        Instant now = clock.instant();
        Shift shift = Shift.start(shifts.nextId(), societyId, actor.userId(), now);
        shifts.save(shift);
        eventPublisher.publish(shift.pullDomainEvents());
        return ShiftView.of(shift, now);
    }

    @Transactional
    public ShiftView end(EndShiftCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        Actor actor = staffOf(societyId);
        Shift shift = shifts.findOpen(societyId, actor.userId()).orElseThrow(NoOpenShiftException::new);
        Instant now = clock.instant();
        shift.end(now);
        shifts.save(shift);
        eventPublisher.publish(shift.pullDomainEvents());
        return ShiftView.of(shift, now);
    }

    private Actor staffOf(SocietyId societyId) {
        Actor actor = currentActor.actor();
        if (!accessPolicy.mayWorkShifts(actor, societyId)) {
            throw new ActionNotAllowedException("Only maintenance staff of this society work shifts");
        }
        return actor;
    }
}
