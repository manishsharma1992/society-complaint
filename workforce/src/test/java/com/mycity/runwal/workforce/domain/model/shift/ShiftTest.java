package com.mycity.runwal.workforce.domain.model.shift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.event.ShiftConfirmed;
import com.mycity.runwal.workforce.domain.event.ShiftDisputed;
import com.mycity.runwal.workforce.domain.event.ShiftEnded;
import com.mycity.runwal.workforce.domain.event.ShiftStarted;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ShiftTest {

    static final SocietyId RUNWAL = new SocietyId(1);
    static final UserId RAJU = new UserId(30);
    static final UserId ADMIN = new UserId(2);
    static final Instant SIX = Instant.parse("2026-11-07T00:30:00Z"); // 06:00 IST
    /** Runwal MyCity, Betwade (plus code 7JFM53PF+CW), 300 m */
    static final Site SITE = new Site(new GeoPoint(19.186062, 73.074812), 300);
    static final Location AT_GATE = new Location(new GeoPoint(19.1865, 73.0750), 12);
    static final Location AT_STATION = new Location(new GeoPoint(19.2183, 73.0868), 20);

    static ClockEvidence selfieAt(Instant at, Location location) {
        return ClockEvidence.at(SITE, SelfieKey.issue(RUNWAL, RAJU, at.minusSeconds(30), "abcdef12"), location);
    }

    static Shift startedAtSix() {
        return Shift.start(new ShiftId(1), RUNWAL, RAJU, SIX, selfieAt(SIX, AT_GATE));
    }

    static Shift endedAfterEightHours() {
        Shift shift = startedAtSix();
        shift.end(SIX.plus(Duration.ofHours(8)), selfieAt(SIX.plus(Duration.ofHours(8)), AT_GATE));
        shift.pullDomainEvents();
        return shift;
    }

    @Nested
    class ClockingInAndOut {

        @Test
        void clockingInOpensAShiftWithItsEvidenceAndRaisesShiftStarted() {
            Shift shift = startedAtSix();

            assertThat(shift.isOpen()).isTrue();
            assertThat(shift.startEvidence()).hasValueSatisfying(e -> assertThat(e.check().isInside()).isTrue());
            assertThat(shift.needsAttention()).isFalse();
            assertThat(shift.pullDomainEvents()).containsExactly(new ShiftStarted(new ShiftId(1), RUNWAL, RAJU, SIX));
        }

        @Test
        void clockingOutEndsItOnceAndRaisesShiftEnded() {
            Shift shift = startedAtSix();
            shift.pullDomainEvents();
            Instant two = SIX.plus(Duration.ofHours(8));

            shift.end(two, selfieAt(two, AT_GATE));

            assertThat(shift.isOpen()).isFalse();
            assertThat(shift.endedAt()).contains(two);
            assertThat(shift.pullDomainEvents()).containsExactly(new ShiftEnded(new ShiftId(1), RUNWAL, RAJU, two));
            assertThatThrownBy(() -> shift.end(two.plusSeconds(60), selfieAt(two.plusSeconds(60), AT_GATE)))
                    .isInstanceOf(InvalidShiftException.class).hasMessageContaining("already ended");
        }

        @Test
        void aShiftCannotEndBeforeItStarted() {
            assertThatThrownBy(() -> startedAtSix().end(SIX.minusSeconds(1), selfieAt(SIX, AT_GATE)))
                    .isInstanceOf(InvalidShiftException.class);
            assertThatThrownBy(() -> Shift.reconstitute(new ShiftId(1), RUNWAL, RAJU, SIX, SIX.minusSeconds(1),
                    null, null, null)).isInstanceOf(InvalidShiftException.class);
        }

        @Test
        void onlyAFreshSelfieOfTheStaffMemberThemselfIsAccepted() {
            ClockEvidence someoneElses = ClockEvidence.at(SITE,
                    SelfieKey.issue(RUNWAL, new UserId(31), SIX.minusSeconds(30), "abcdef12"), AT_GATE);
            ClockEvidence otherSociety = ClockEvidence.at(SITE,
                    SelfieKey.issue(new SocietyId(2), RAJU, SIX.minusSeconds(30), "abcdef12"), AT_GATE);
            ClockEvidence yesterdays = ClockEvidence.at(SITE,
                    SelfieKey.issue(RUNWAL, RAJU, SIX.minus(Duration.ofDays(1)), "abcdef12"), AT_GATE);
            ClockEvidence noSelfie = new ClockEvidence(null, AT_GATE, SITE.check(AT_GATE));

            for (ClockEvidence bad : new ClockEvidence[] {someoneElses, otherSociety, yesterdays, noSelfie}) {
                assertThatThrownBy(() -> Shift.start(new ShiftId(1), RUNWAL, RAJU, SIX, bad))
                        .isInstanceOf(InvalidShiftException.class);
            }
            Shift shift = startedAtSix();
            Instant later = SIX.plus(Duration.ofHours(8));
            assertThatThrownBy(() -> shift.end(later, selfieAt(SIX, AT_GATE)))
                    .isInstanceOf(InvalidShiftException.class).hasMessageContaining("new selfie");
        }

        @Test
        void clockingInOutsideTheSiteOrWithoutALocationIsAllowedButNeedsAttention() {
            Shift outside = Shift.start(new ShiftId(1), RUNWAL, RAJU, SIX, selfieAt(SIX, AT_STATION));
            Shift noLocation = Shift.start(new ShiftId(2), RUNWAL, RAJU, SIX, selfieAt(SIX, null));
            Shift endsOutside = startedAtSix();
            endsOutside.end(SIX.plus(Duration.ofHours(8)), selfieAt(SIX.plus(Duration.ofHours(8)), AT_STATION));

            assertThat(outside.needsAttention()).isTrue();
            assertThat(noLocation.needsAttention()).isTrue();
            assertThat(endsOutside.needsAttention()).isTrue();
            assertThat(endedAfterEightHours().needsAttention()).isFalse();
            assertThat(Shift.reconstitute(new ShiftId(3), RUNWAL, RAJU, SIX, null, null, null, null).needsAttention())
                    .isTrue();
        }
    }

    @Nested
    class Review {

        @Test
        void anAdminConfirmsOrDisputesAnEndedShiftAndTheLaterReviewWins() {
            Shift shift = endedAfterEightHours();
            Instant later = SIX.plus(Duration.ofHours(9));

            shift.confirm(ADMIN, later);
            assertThat(shift.review()).hasValueSatisfying(r -> assertThat(r.decision()).isEqualTo(ShiftReview.Decision.CONFIRMED));
            assertThat(shift.pullDomainEvents()).containsExactly(new ShiftConfirmed(new ShiftId(1), RUNWAL, RAJU, ADMIN, later));

            shift.dispute(ADMIN, later.plusSeconds(60), " Not Raju in the evening selfie ");
            assertThat(shift.review()).hasValueSatisfying(r -> {
                assertThat(r.decision()).isEqualTo(ShiftReview.Decision.DISPUTED);
                assertThat(r.reason()).isEqualTo("Not Raju in the evening selfie");
            });
            assertThat(shift.pullDomainEvents()).hasExactlyElementsOfTypes(ShiftDisputed.class);
        }

        @Test
        void aDisputeNeedsAReasonAndNobodyReviewsAnOpenShiftOrTheirOwn() {
            Shift ended = endedAfterEightHours();
            Instant later = SIX.plus(Duration.ofHours(9));

            assertThatThrownBy(() -> ended.dispute(ADMIN, later, "  ")).isInstanceOf(InvalidShiftException.class);
            assertThatThrownBy(() -> ended.confirm(RAJU, later))
                    .isInstanceOf(InvalidShiftException.class).hasMessageContaining("own");
            assertThatThrownBy(() -> startedAtSix().confirm(ADMIN, later))
                    .isInstanceOf(InvalidShiftException.class).hasMessageContaining("ended");
        }
    }

    @Nested
    class Retention {

        @Test
        void forgettingRemovesSelfiesAndLocationsButKeepsTimesChecksAndReview() {
            Shift shift = endedAfterEightHours();
            shift.confirm(ADMIN, SIX.plus(Duration.ofHours(9)));
            assertThat(shift.holdsPersonalEvidence()).isTrue();

            shift.forgetEvidence();

            assertThat(shift.holdsPersonalEvidence()).isFalse();
            assertThat(shift.startEvidence()).hasValueSatisfying(e -> {
                assertThat(e.selfieKey()).isEmpty();
                assertThat(e.where()).isEmpty();
                assertThat(e.check().placement()).isEqualTo(Placement.INSIDE);
            });
            assertThat(shift.endedAt()).isPresent();
            assertThat(shift.review()).isPresent();
            assertThat(shift.needsAttention()).isFalse();
        }
    }

    @Nested
    class Hours {

        @Test
        void onlyThePartInsideThePeriodCounts() {
            Shift shift = endedAfterEightHours();

            assertThat(shift.workedWithin(SIX.minus(Duration.ofDays(1)), SIX.plus(Duration.ofDays(1)), SIX))
                    .isEqualTo(Duration.ofHours(8));
            assertThat(shift.workedWithin(SIX.plus(Duration.ofHours(6)), SIX.plus(Duration.ofDays(1)), SIX))
                    .isEqualTo(Duration.ofHours(2));
            assertThat(shift.workedWithin(SIX.plus(Duration.ofHours(9)), SIX.plus(Duration.ofDays(1)), SIX))
                    .isEqualTo(Duration.ZERO);
        }

        @Test
        void anOpenShiftCountsUntilNowButAForgottenClockOutIsCapped() {
            Shift shift = startedAtSix();

            assertThat(shift.workedWithin(SIX, Instant.MAX, SIX.plus(Duration.ofHours(3))))
                    .isEqualTo(Duration.ofHours(3));
            assertThat(shift.workedWithin(SIX, Instant.MAX, SIX.plus(Duration.ofDays(3))))
                    .isEqualTo(Shift.MAX_COUNTED);
        }

        @Test
        void aReportPeriodMustBeForwardAndAtMostAboutAYear() {
            assertThatThrownBy(() -> new Period(SIX, SIX)).isInstanceOf(InvalidShiftException.class);
            assertThatThrownBy(() -> new Period(SIX, SIX.plus(Duration.ofDays(401))))
                    .isInstanceOf(InvalidShiftException.class);
            assertThat(new Period(SIX, SIX.plus(Duration.ofDays(366))).from()).isEqualTo(SIX);
        }
    }
}
