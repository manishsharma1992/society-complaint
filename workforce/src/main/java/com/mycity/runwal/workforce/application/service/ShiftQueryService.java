package com.mycity.runwal.workforce.application.service;

import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.workforce.application.port.CurrentActor;
import com.mycity.runwal.workforce.application.port.SelfieStorage;
import com.mycity.runwal.workforce.application.query.MyShiftsQuery;
import com.mycity.runwal.workforce.application.query.OnDutyQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursView;
import com.mycity.runwal.workforce.application.query.ShiftView;
import com.mycity.runwal.workforce.application.query.ShiftsQuery;
import com.mycity.runwal.workforce.application.query.StaffOnDutyQuery;
import com.mycity.runwal.workforce.domain.model.actor.Actor;
import com.mycity.runwal.workforce.domain.model.shift.Period;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.ShiftRepository;
import com.mycity.runwal.workforce.domain.service.WorkforceAccessPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/** Reads shifts: staff see their own, admins see everyone's. */
@ApplicationService
public class ShiftQueryService {

    private final ShiftRepository shifts;
    private final CurrentActor currentActor;
    private final WorkforceAccessPolicy accessPolicy;
    private final SelfieStorage selfies;
    private final Clock clock;

    public ShiftQueryService(ShiftRepository shifts, CurrentActor currentActor, WorkforceAccessPolicy accessPolicy,
                             SelfieStorage selfies, Clock clock) {
        this.shifts = shifts;
        this.currentActor = currentActor;
        this.accessPolicy = accessPolicy;
        this.selfies = selfies;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ShiftView> mine(MyShiftsQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        Actor actor = currentActor.actor();
        if (!accessPolicy.mayWorkShifts(actor, societyId)) {
            throw new ActionNotAllowedException("Only maintenance staff of this society have shifts");
        }
        Period period = new Period(query.from(), query.to());
        Instant now = clock.instant();
        return shifts.findOverlapping(societyId, period.from(), period.to()).stream()
                .filter(shift -> shift.staffId().equals(actor.userId()))
                .sorted(Comparator.comparing(Shift::startedAt).reversed())
                .map(shift -> ShiftView.of(shift, now, selfies))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftView> onDuty(OnDutyQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        if (!accessPolicy.maySeeAllShifts(currentActor.actor(), societyId)) {
            throw new ActionNotAllowedException("Only the society admin or a tower admin can see who is on duty");
        }
        Instant now = clock.instant();
        return shifts.findAllOpen(societyId).stream()
                .sorted(Comparator.comparing(Shift::startedAt))
                .map(shift -> ShiftView.of(shift, now, selfies))
                .toList();
    }

    /** For other contexts (no caller check): the staff with an open shift, e.g. for automatic assignment. */
    @Transactional(readOnly = true)
    public List<Long> staffOnDuty(StaffOnDutyQuery query) {
        return shifts.findAllOpen(new SocietyId(query.societyId())).stream()
                .map(shift -> shift.staffId().value())
                .distinct()
                .toList();
    }

    /** Every staff member's shifts in the period, newest first, with their evidence and review (admins). */
    @Transactional(readOnly = true)
    public List<ShiftView> list(ShiftsQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        if (!accessPolicy.maySeeAllShifts(currentActor.actor(), societyId)) {
            throw new ActionNotAllowedException("Only the society admin or a tower admin can review shifts");
        }
        Period period = new Period(query.from(), query.to());
        Instant now = clock.instant();
        return shifts.findOverlapping(societyId, period.from(), period.to()).stream()
                .sorted(Comparator.comparing(Shift::startedAt).reversed())
                .map(shift -> ShiftView.of(shift, now, selfies))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftHoursView> hours(ShiftHoursQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        Actor actor = currentActor.actor();
        boolean all = accessPolicy.maySeeAllShifts(actor, societyId);
        if (!all && !accessPolicy.mayWorkShifts(actor, societyId)) {
            throw new ActionNotAllowedException("Only admins and staff of this society can see shift hours");
        }
        Period period = new Period(query.from(), query.to());
        Instant now = clock.instant();
        Map<Long, Totals> totals = new LinkedHashMap<>();
        for (Shift shift : shifts.findOverlapping(societyId, period.from(), period.to())) {
            if (!all && !shift.staffId().equals(actor.userId())) {
                continue;
            }
            Totals staff = totals.computeIfAbsent(shift.staffId().value(), id -> new Totals());
            staff.shifts++;
            staff.worked = staff.worked.plus(shift.workedWithin(period.from(), period.to(), now));
            staff.onDutyNow |= shift.isOpen();
        }
        return totals.entrySet().stream()
                .map(e -> new ShiftHoursView(e.getKey(), e.getValue().shifts, e.getValue().worked.toMinutes(),
                        e.getValue().onDutyNow))
                .toList();
    }

    private static final class Totals {
        private int shifts;
        private Duration worked = Duration.ZERO;
        private boolean onDutyNow;
    }
}
