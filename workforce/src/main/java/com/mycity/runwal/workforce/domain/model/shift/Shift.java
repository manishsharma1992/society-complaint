package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.AbstractAggregateRoot;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.event.ShiftEnded;
import com.mycity.runwal.workforce.domain.event.ShiftStarted;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One period a maintenance staff member was on duty: they clock in, and later clock out.
 * A staff member has at most one open shift at a time (checked by the application service and the database).
 */
@AggregateRoot
public class Shift extends AbstractAggregateRoot {

    /**
     * A shift counts for at most this long, so a forgotten clock-out cannot inflate the hours worked.
     * Time past it is not counted until an admin corrects the shift.
     */
    public static final Duration MAX_COUNTED = Duration.ofHours(16);

    private final ShiftId id;
    private final SocietyId societyId;
    private final UserId staffId;
    private final Instant startedAt;
    private Instant endedAt;

    private Shift(ShiftId id, SocietyId societyId, UserId staffId, Instant startedAt, Instant endedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.societyId = Objects.requireNonNull(societyId, "societyId");
        this.staffId = Objects.requireNonNull(staffId, "staffId");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        if (endedAt != null && endedAt.isBefore(startedAt)) {
            throw new InvalidShiftException("A shift cannot end before it started");
        }
        this.endedAt = endedAt;
    }

    /** The staff member clocks in. */
    public static Shift start(ShiftId id, SocietyId societyId, UserId staffId, Instant at) {
        Shift shift = new Shift(id, societyId, staffId, at, null);
        shift.recordEvent(new ShiftStarted(id, societyId, staffId, at));
        return shift;
    }

    /** Rebuilds a stored shift; raises no events. */
    public static Shift reconstitute(ShiftId id, SocietyId societyId, UserId staffId, Instant startedAt,
                                     Instant endedAt) {
        return new Shift(id, societyId, staffId, startedAt, endedAt);
    }

    /** The staff member clocks out. */
    public void end(Instant at) {
        if (endedAt != null) {
            throw new InvalidShiftException("This shift has already ended");
        }
        if (at.isBefore(startedAt)) {
            throw new InvalidShiftException("A shift cannot end before it started");
        }
        endedAt = at;
        recordEvent(new ShiftEnded(id, societyId, staffId, at));
    }

    public boolean isOpen() {
        return endedAt == null;
    }

    /**
     * How much of this shift falls in [from, to): an open shift runs until {@code now}, and no shift counts for
     * more than {@link #MAX_COUNTED} from its start.
     */
    public Duration workedWithin(Instant from, Instant to, Instant now) {
        Instant end = endedAt != null ? endedAt : now;
        Instant countedEnd = min(end, startedAt.plus(MAX_COUNTED));
        Instant overlapStart = startedAt.isAfter(from) ? startedAt : from;
        Instant overlapEnd = min(countedEnd, to);
        return overlapEnd.isAfter(overlapStart) ? Duration.between(overlapStart, overlapEnd) : Duration.ZERO;
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    public ShiftId id() {
        return id;
    }

    public SocietyId societyId() {
        return societyId;
    }

    public UserId staffId() {
        return staffId;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Optional<Instant> endedAt() {
        return Optional.ofNullable(endedAt);
    }
}
