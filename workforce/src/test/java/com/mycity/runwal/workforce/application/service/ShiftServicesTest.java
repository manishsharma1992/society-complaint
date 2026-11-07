package com.mycity.runwal.workforce.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.application.command.EndShiftCommand;
import com.mycity.runwal.workforce.application.command.StartShiftCommand;
import com.mycity.runwal.workforce.application.query.MyShiftsQuery;
import com.mycity.runwal.workforce.application.query.OnDutyQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursView;
import com.mycity.runwal.workforce.domain.event.ShiftEnded;
import com.mycity.runwal.workforce.domain.event.ShiftStarted;
import com.mycity.runwal.workforce.domain.model.actor.Actor;
import com.mycity.runwal.workforce.domain.model.actor.ActorRole;
import com.mycity.runwal.workforce.domain.model.actor.ActorRole.Part;
import com.mycity.runwal.workforce.domain.service.WorkforceAccessPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShiftServicesTest {

    private static final SocietyId RUNWAL = new SocietyId(1);
    private static final Instant SIX = Instant.parse("2026-11-07T00:30:00Z");
    private static final Instant DAY_START = Instant.parse("2026-11-06T18:30:00Z");
    private static final Instant DAY_END = DAY_START.plus(Duration.ofDays(1));

    private static final Actor RAJU = actor(30, Part.STAFF);
    private static final Actor SUNIL = actor(31, Part.STAFF);
    private static final Actor ADMIN = actor(1, Part.SOCIETY_ADMIN);
    private static final Actor TOWER_ADMIN = actor(2, Part.TOWER_ADMIN);
    private static final Actor RESIDENT = actor(10, Part.RESIDENT);

    private final InMemoryShifts shifts = new InMemoryShifts();
    private final MutableClock clock = new MutableClock(SIX);
    private final List<DomainEvent> published = new ArrayList<>();
    private Actor current = RAJU;
    private final WorkforceAccessPolicy policy = new WorkforceAccessPolicy();
    private final ShiftService shiftService = new ShiftService(shifts, () -> current, policy, published::addAll, clock);
    private final ShiftQueryService queryService = new ShiftQueryService(shifts, () -> current, policy, clock);

    private static Actor actor(long id, Part part) {
        return new Actor(new UserId(id), true, Set.of(new ActorRole(part, RUNWAL)));
    }

    private void as(Actor actor) {
        current = actor;
    }

    @Test
    void staffClockInAndOutAndTheEventsArePublished() {
        var started = shiftService.start(new StartShiftCommand(1));
        clock.advance(Duration.ofHours(8));
        var ended = shiftService.end(new EndShiftCommand(1));

        assertThat(started.endedAt()).isNull();
        assertThat(ended.endedAt()).isEqualTo(SIX.plus(Duration.ofHours(8)));
        assertThat(ended.minutesCounted()).isEqualTo(8 * 60);
        assertThat(published).hasExactlyElementsOfTypes(ShiftStarted.class, ShiftEnded.class);
    }

    @Test
    void staffCannotClockInTwiceOrClockOutWithoutAShift() {
        assertThatThrownBy(() -> shiftService.end(new EndShiftCommand(1))).isInstanceOf(NoOpenShiftException.class);
        shiftService.start(new StartShiftCommand(1));
        assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(1)))
                .isInstanceOf(ShiftAlreadyOpenException.class);
    }

    @Test
    void onlyActiveStaffOfThisSocietyWorkShifts() {
        as(RESIDENT);
        assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(1)))
                .isInstanceOf(ActionNotAllowedException.class);
        as(ADMIN);
        assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(1)))
                .isInstanceOf(ActionNotAllowedException.class);
        as(RAJU);
        assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(2)))
                .isInstanceOf(ActionNotAllowedException.class);
        as(new Actor(new UserId(30), false, RAJU.roles()));
        assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(1)))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void adminsSeeWhoIsOnDutyAndEveryonesHours() {
        shiftService.start(new StartShiftCommand(1));
        clock.advance(Duration.ofHours(8));
        shiftService.end(new EndShiftCommand(1));
        as(SUNIL);
        shiftService.start(new StartShiftCommand(1));
        clock.advance(Duration.ofHours(2));

        for (Actor admin : List.of(ADMIN, TOWER_ADMIN)) {
            as(admin);
            assertThat(queryService.onDuty(new OnDutyQuery(1))).extracting(v -> v.staffId()).containsExactly(31L);
            assertThat(queryService.hours(new ShiftHoursQuery(1, DAY_START, DAY_END))).containsExactlyInAnyOrder(
                    new ShiftHoursView(30, 1, 8 * 60, false),
                    new ShiftHoursView(31, 1, 2 * 60, true));
        }
    }

    @Test
    void staffSeeOnlyTheirOwnShiftsAndHours() {
        shiftService.start(new StartShiftCommand(1));
        as(SUNIL);
        shiftService.start(new StartShiftCommand(1));
        clock.advance(Duration.ofHours(1));

        assertThat(queryService.hours(new ShiftHoursQuery(1, DAY_START, DAY_END)))
                .containsExactly(new ShiftHoursView(31, 1, 60, true));
        assertThat(queryService.mine(new MyShiftsQuery(1, DAY_START, DAY_END)))
                .extracting(v -> v.staffId()).containsExactly(31L);
        assertThatThrownBy(() -> queryService.onDuty(new OnDutyQuery(1)))
                .isInstanceOf(ActionNotAllowedException.class);
        as(RESIDENT);
        assertThatThrownBy(() -> queryService.hours(new ShiftHoursQuery(1, DAY_START, DAY_END)))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
