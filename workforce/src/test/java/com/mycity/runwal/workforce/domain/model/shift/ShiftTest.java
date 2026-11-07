package com.mycity.runwal.workforce.domain.model.shift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.event.ShiftEnded;
import com.mycity.runwal.workforce.domain.event.ShiftStarted;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ShiftTest {

    private static final SocietyId RUNWAL = new SocietyId(1);
    private static final UserId RAJU = new UserId(30);
    private static final Instant SIX = Instant.parse("2026-11-07T00:30:00Z"); // 06:00 IST

    private static Shift startedAtSix() {
        return Shift.start(new ShiftId(1), RUNWAL, RAJU, SIX);
    }

    @Test
    void clockingInOpensAShiftAndRaisesShiftStarted() {
        Shift shift = startedAtSix();

        assertThat(shift.isOpen()).isTrue();
        assertThat(shift.pullDomainEvents()).containsExactly(new ShiftStarted(new ShiftId(1), RUNWAL, RAJU, SIX));
    }

    @Test
    void clockingOutEndsItOnceAndRaisesShiftEnded() {
        Shift shift = startedAtSix();
        shift.pullDomainEvents();
        Instant two = SIX.plus(Duration.ofHours(8));

        shift.end(two);

        assertThat(shift.isOpen()).isFalse();
        assertThat(shift.endedAt()).contains(two);
        assertThat(shift.pullDomainEvents()).containsExactly(new ShiftEnded(new ShiftId(1), RUNWAL, RAJU, two));
        assertThatThrownBy(() -> shift.end(two.plusSeconds(60)))
                .isInstanceOf(InvalidShiftException.class).hasMessageContaining("already ended");
    }

    @Test
    void aShiftCannotEndBeforeItStarted() {
        assertThatThrownBy(() -> startedAtSix().end(SIX.minusSeconds(1))).isInstanceOf(InvalidShiftException.class);
        assertThatThrownBy(() -> Shift.reconstitute(new ShiftId(1), RUNWAL, RAJU, SIX, SIX.minusSeconds(1)))
                .isInstanceOf(InvalidShiftException.class);
    }

    @Test
    void onlyThePartInsideThePeriodCounts() {
        Shift shift = Shift.reconstitute(new ShiftId(1), RUNWAL, RAJU, SIX, SIX.plus(Duration.ofHours(8)));

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
