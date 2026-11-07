package com.mycity.runwal.workforce.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.application.command.EndShiftCommand;
import com.mycity.runwal.workforce.application.command.RequestSelfieUploadCommand;
import com.mycity.runwal.workforce.application.command.ReviewShiftCommand;
import com.mycity.runwal.workforce.application.command.ReviewShiftCommand.Decision;
import com.mycity.runwal.workforce.application.command.StartShiftCommand;
import com.mycity.runwal.workforce.application.query.MyShiftsQuery;
import com.mycity.runwal.workforce.application.query.OnDutyQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursView;
import com.mycity.runwal.workforce.application.query.ShiftView;
import com.mycity.runwal.workforce.application.query.ShiftsQuery;
import com.mycity.runwal.workforce.domain.event.ShiftConfirmed;
import com.mycity.runwal.workforce.domain.event.ShiftEnded;
import com.mycity.runwal.workforce.domain.event.ShiftStarted;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import com.mycity.runwal.workforce.domain.exception.SelfieNotUploadedException;
import com.mycity.runwal.workforce.domain.model.actor.Actor;
import com.mycity.runwal.workforce.domain.model.actor.ActorRole;
import com.mycity.runwal.workforce.domain.model.actor.ActorRole.Part;
import com.mycity.runwal.workforce.domain.model.shift.GeoPoint;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.Site;
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
    private static final Site SITE = new Site(new GeoPoint(19.186062, 73.074812), 300);
    private static final double[] GATE = {19.1865, 73.0750, 12};
    private static final double[] STATION = {19.2183, 73.0868, 20};

    private static final Actor RAJU = actor(30, Part.STAFF);
    private static final Actor SUNIL = actor(31, Part.STAFF);
    private static final Actor ADMIN = actor(1, Part.SOCIETY_ADMIN);
    private static final Actor TOWER_ADMIN = actor(2, Part.TOWER_ADMIN);
    private static final Actor RESIDENT = actor(10, Part.RESIDENT);

    private final InMemoryShifts shifts = new InMemoryShifts();
    private final FakeSelfieStorage selfies = new FakeSelfieStorage();
    private final MutableClock clock = new MutableClock(SIX);
    private final List<DomainEvent> published = new ArrayList<>();
    private Actor current = RAJU;
    private final WorkforceAccessPolicy policy = new WorkforceAccessPolicy();
    private final ShiftService shiftService = new ShiftService(shifts, () -> current, policy, published::addAll,
            selfies, society -> SITE, clock);
    private final ShiftQueryService queryService = new ShiftQueryService(shifts, () -> current, policy, selfies, clock);
    private final ShiftReviewService reviewService = new ShiftReviewService(shifts, () -> current, policy,
            published::addAll, selfies, clock);
    private final ShiftEvidenceRetentionService retention = new ShiftEvidenceRetentionService(shifts, selfies, clock);

    private static Actor actor(long id, Part part) {
        return new Actor(new UserId(id), true, Set.of(new ActorRole(part, RUNWAL)));
    }

    private void as(Actor actor) {
        current = actor;
    }

    /** What the phone does: ask for an upload link, upload the selfie, return its key. */
    private String takeSelfie() {
        String key = shiftService.requestSelfieUpload(new RequestSelfieUploadCommand(1, 150_000)).storageKey();
        selfies.put(key);
        return key;
    }

    private ShiftView clockIn(double[] where) {
        return shiftService.start(where == null ? new StartShiftCommand(1, takeSelfie(), null, null, null)
                : new StartShiftCommand(1, takeSelfie(), where[0], where[1], where[2]));
    }

    private ShiftView clockOut(double[] where) {
        return shiftService.end(new EndShiftCommand(1, takeSelfie(), where[0], where[1], where[2]));
    }

    @Test
    void staffClockInAndOutWithSelfiesAndLocationsAndTheEventsArePublished() {
        ShiftView started = clockIn(GATE);
        clock.advance(Duration.ofHours(8));
        ShiftView ended = clockOut(GATE);

        assertThat(started.endedAt()).isNull();
        assertThat(started.start().placement()).isEqualTo("INSIDE");
        assertThat(started.start().selfieUrl()).startsWith("https://bucket.test/shifts/1/30/").endsWith("?get");
        assertThat(ended.endedAt()).isEqualTo(SIX.plus(Duration.ofHours(8)));
        assertThat(ended.minutesCounted()).isEqualTo(8 * 60);
        assertThat(ended.end().latitude()).isEqualTo(19.1865);
        assertThat(ended.needsAttention()).isFalse();
        assertThat(published).hasExactlyElementsOfTypes(ShiftStarted.class, ShiftEnded.class);
    }

    @Test
    void theUploadLinkIsForASmallJpegUnderTheStaffMembersOwnKey() {
        var upload = shiftService.requestSelfieUpload(new RequestSelfieUploadCommand(1, 150_000));

        assertThat(upload.storageKey()).startsWith("shifts/1/30/" + SIX.toEpochMilli() + "-").endsWith(".jpg");
        assertThat(upload.contentType()).isEqualTo("image/jpeg");
        assertThat(upload.expiresAt()).isEqualTo(SIX.plus(Duration.ofMinutes(10)));
        assertThatThrownBy(() -> shiftService.requestSelfieUpload(new RequestSelfieUploadCommand(1, 3 * 1024 * 1024)))
                .isInstanceOf(InvalidShiftException.class);
        assertThatThrownBy(() -> shiftService.requestSelfieUpload(new RequestSelfieUploadCommand(1, 0)))
                .isInstanceOf(InvalidShiftException.class);
    }

    @Test
    void aSelfieThatWasNeverUploadedOrIsSomeoneElsesIsRefused() {
        String notUploaded = shiftService.requestSelfieUpload(new RequestSelfieUploadCommand(1, 150_000)).storageKey();
        assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(1, notUploaded, GATE[0], GATE[1], GATE[2])))
                .isInstanceOf(SelfieNotUploadedException.class);

        as(SUNIL);
        String sunils = takeSelfie();
        as(RAJU);
        assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(1, sunils, GATE[0], GATE[1], GATE[2])))
                .isInstanceOf(InvalidShiftException.class).hasMessageContaining("someone else");
        assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(1, null, GATE[0], GATE[1], GATE[2])))
                .isInstanceOf(InvalidShiftException.class);
        assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(1, takeSelfie(), GATE[0], null, GATE[2])))
                .isInstanceOf(InvalidShiftException.class).hasMessageContaining("latitude, longitude and accuracy");
    }

    @Test
    void anOldSelfieCannotBeReused() {
        String key = takeSelfie();
        clock.advance(Duration.ofMinutes(11));

        assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(1, key, GATE[0], GATE[1], GATE[2])))
                .isInstanceOf(InvalidShiftException.class).hasMessageContaining("new selfie");
    }

    @Test
    void outsideTheSiteOrWithoutLocationIsRecordedAndFlaggedNotRefused() {
        ShiftView outside = clockIn(STATION);
        assertThat(outside.start().placement()).isEqualTo("OUTSIDE");
        assertThat(outside.start().distanceMeters()).isBetween(3_500L, 4_000L);
        assertThat(outside.needsAttention()).isTrue();

        as(SUNIL);
        ShiftView noLocation = clockIn(null);
        assertThat(noLocation.start().placement()).isEqualTo("NO_LOCATION");
        assertThat(noLocation.start().latitude()).isNull();
        assertThat(noLocation.needsAttention()).isTrue();
    }

    @Test
    void staffCannotClockInTwiceOrClockOutWithoutAShift() {
        assertThatThrownBy(() -> clockOut(GATE)).isInstanceOf(NoOpenShiftException.class);
        clockIn(GATE);
        assertThatThrownBy(() -> clockIn(GATE)).isInstanceOf(ShiftAlreadyOpenException.class);
    }

    @Test
    void onlyActiveStaffOfThisSocietyWorkShifts() {
        for (Actor outsider : List.of(RESIDENT, ADMIN, new Actor(new UserId(30), false, RAJU.roles()))) {
            as(outsider);
            assertThatThrownBy(() -> shiftService.requestSelfieUpload(new RequestSelfieUploadCommand(1, 150_000)))
                    .isInstanceOf(ActionNotAllowedException.class);
            assertThatThrownBy(() -> shiftService.start(new StartShiftCommand(1, "x", null, null, null)))
                    .isInstanceOf(ActionNotAllowedException.class);
        }
        as(RAJU);
        assertThatThrownBy(() -> shiftService.requestSelfieUpload(new RequestSelfieUploadCommand(2, 150_000)))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void adminsSeeWhoIsOnDutyEveryonesHoursAndTheShiftsToReview() {
        clockIn(GATE);
        clock.advance(Duration.ofHours(8));
        clockOut(GATE);
        as(SUNIL);
        clockIn(STATION);
        clock.advance(Duration.ofHours(2));

        for (Actor admin : List.of(ADMIN, TOWER_ADMIN)) {
            as(admin);
            assertThat(queryService.onDuty(new OnDutyQuery(1))).extracting(ShiftView::staffId).containsExactly(31L);
            assertThat(queryService.hours(new ShiftHoursQuery(1, DAY_START, DAY_END))).containsExactlyInAnyOrder(
                    new ShiftHoursView(30, 1, 8 * 60, false),
                    new ShiftHoursView(31, 1, 2 * 60, true));
            assertThat(queryService.list(new ShiftsQuery(1, DAY_START, DAY_END)))
                    .extracting(ShiftView::staffId, ShiftView::needsAttention)
                    .containsExactly(org.assertj.core.groups.Tuple.tuple(31L, true),
                            org.assertj.core.groups.Tuple.tuple(30L, false));
        }
    }

    @Test
    void staffSeeOnlyTheirOwnShiftsAndHours() {
        clockIn(GATE);
        as(SUNIL);
        clockIn(GATE);
        clock.advance(Duration.ofHours(1));

        assertThat(queryService.hours(new ShiftHoursQuery(1, DAY_START, DAY_END)))
                .containsExactly(new ShiftHoursView(31, 1, 60, true));
        assertThat(queryService.mine(new MyShiftsQuery(1, DAY_START, DAY_END)))
                .extracting(ShiftView::staffId).containsExactly(31L);
        assertThatThrownBy(() -> queryService.onDuty(new OnDutyQuery(1)))
                .isInstanceOf(ActionNotAllowedException.class);
        assertThatThrownBy(() -> queryService.list(new ShiftsQuery(1, DAY_START, DAY_END)))
                .isInstanceOf(ActionNotAllowedException.class);
        as(RESIDENT);
        assertThatThrownBy(() -> queryService.hours(new ShiftHoursQuery(1, DAY_START, DAY_END)))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void towerAndSocietyAdminsConfirmOrDisputeEndedShifts() {
        long id = clockIn(GATE).id();
        clock.advance(Duration.ofHours(8));
        clockOut(STATION);
        published.clear();

        as(TOWER_ADMIN);
        ShiftView confirmed = reviewService.review(new ReviewShiftCommand(1, id, Decision.CONFIRM, null)).orElseThrow();
        assertThat(confirmed.review().decision()).isEqualTo("CONFIRMED");
        assertThat(confirmed.review().reviewerId()).isEqualTo(2);
        assertThat(published).hasExactlyElementsOfTypes(ShiftConfirmed.class);

        as(ADMIN);
        ShiftView disputed = reviewService.review(
                new ReviewShiftCommand(1, id, Decision.DISPUTE, "Clocked out at Dombivli station")).orElseThrow();
        assertThat(disputed.review().decision()).isEqualTo("DISPUTED");
        assertThat(disputed.review().reason()).isEqualTo("Clocked out at Dombivli station");
        assertThat(reviewService.review(new ReviewShiftCommand(1, 999, Decision.CONFIRM, null))).isEmpty();
        assertThatThrownBy(() -> reviewService.review(new ReviewShiftCommand(2, id, Decision.CONFIRM, null)))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void staffAndResidentsCannotReviewShifts() {
        long id = clockIn(GATE).id();
        clock.advance(Duration.ofHours(8));
        clockOut(GATE);

        for (Actor notAdmin : List.of(RAJU, SUNIL, RESIDENT)) {
            as(notAdmin);
            assertThatThrownBy(() -> reviewService.review(new ReviewShiftCommand(1, id, Decision.CONFIRM, null)))
                    .isInstanceOf(ActionNotAllowedException.class);
        }
    }

    @Test
    void selfiesAndLocationsAreDeletedAfter90DaysButTheShiftStays() {
        long id = clockIn(GATE).id();
        clock.advance(Duration.ofHours(8));
        clockOut(GATE);
        as(SUNIL);
        clock.advance(Duration.ofDays(30));
        clockIn(GATE);
        assertThat(selfies.stored).hasSize(3);

        clock.advance(Duration.ofDays(61)); // Raju's shift is now 91 days old, Sunil's 61
        assertThat(retention.forgetExpiredBatch()).isEqualTo(1);
        assertThat(retention.forgetExpiredBatch()).isZero();

        Shift rajus = shifts.findById(RUNWAL, new com.mycity.runwal.workforce.domain.model.shift.ShiftId(id)).orElseThrow();
        assertThat(rajus.holdsPersonalEvidence()).isFalse();
        assertThat(rajus.endedAt()).isPresent();
        assertThat(rajus.startEvidence()).hasValueSatisfying(e -> assertThat(e.check().isInside()).isTrue());
        assertThat(selfies.stored).hasSize(1).allSatisfy((key, size) -> assertThat(key).startsWith("shifts/1/31/"));
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
