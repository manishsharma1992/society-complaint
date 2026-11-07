package com.mycity.runwal.workforce.application.service;

import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.workforce.application.command.ReviewShiftCommand;
import com.mycity.runwal.workforce.application.port.CurrentActor;
import com.mycity.runwal.workforce.application.port.DomainEventPublisher;
import com.mycity.runwal.workforce.application.port.SelfieStorage;
import com.mycity.runwal.workforce.application.query.ShiftView;
import com.mycity.runwal.workforce.domain.model.actor.Actor;
import com.mycity.runwal.workforce.domain.model.shift.ShiftId;
import com.mycity.runwal.workforce.domain.model.shift.ShiftRepository;
import com.mycity.runwal.workforce.domain.service.WorkforceAccessPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** Tower admins and the society admin confirm or dispute shifts after comparing the selfies with the staff member. */
@ApplicationService
public class ShiftReviewService {

    private final ShiftRepository shifts;
    private final CurrentActor currentActor;
    private final WorkforceAccessPolicy accessPolicy;
    private final DomainEventPublisher eventPublisher;
    private final SelfieStorage selfies;
    private final Clock clock;

    public ShiftReviewService(ShiftRepository shifts, CurrentActor currentActor, WorkforceAccessPolicy accessPolicy,
                              DomainEventPublisher eventPublisher, SelfieStorage selfies, Clock clock) {
        this.shifts = shifts;
        this.currentActor = currentActor;
        this.accessPolicy = accessPolicy;
        this.eventPublisher = eventPublisher;
        this.selfies = selfies;
        this.clock = clock;
    }

    /** Empty when the society has no such shift. */
    @Transactional
    public Optional<ShiftView> review(ReviewShiftCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        Actor actor = currentActor.actor();
        if (!accessPolicy.maySeeAllShifts(actor, societyId)) {
            throw new ActionNotAllowedException("Only the society admin or a tower admin can review shifts");
        }
        Instant now = clock.instant();
        return shifts.findById(societyId, new ShiftId(command.shiftId())).map(shift -> {
            switch (command.decision()) {
                case CONFIRM -> shift.confirm(actor.userId(), now);
                case DISPUTE -> shift.dispute(actor.userId(), now, command.reason());
            }
            shifts.save(shift);
            eventPublisher.publish(shift.pullDomainEvents());
            return ShiftView.of(shift, now, selfies);
        });
    }
}
