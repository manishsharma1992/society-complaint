package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.AbstractAggregateRoot;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.event.ShiftConfirmed;
import com.mycity.runwal.workforce.domain.event.ShiftDisputed;
import com.mycity.runwal.workforce.domain.event.ShiftEnded;
import com.mycity.runwal.workforce.domain.event.ShiftStarted;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One period a maintenance staff member was on duty: they clock in, and later clock out, each time with a live
 * selfie and the phone's location as evidence. An admin then confirms or disputes the shift by checking the
 * selfies (is it really them?) and where they were.
 * A staff member has at most one open shift at a time (checked by the application service and the database).
 */
@AggregateRoot
public class Shift extends AbstractAggregateRoot {

    /**
     * A shift counts for at most this long, so a forgotten clock-out cannot inflate the hours worked.
     * Time past it is not counted until an admin corrects the shift.
     */
    public static final Duration MAX_COUNTED = Duration.ofHours(16);

    /** Selfies and locations are personal data: they are kept this long after the shift started, then forgotten. */
    public static final Duration EVIDENCE_KEPT_FOR = Duration.ofDays(90);

    private final ShiftId id;
    private final SocietyId societyId;
    private final UserId staffId;
    private final Instant startedAt;
    private Instant endedAt;
    /** null only for shifts recorded before evidence was required */
    private ClockEvidence startEvidence;
    private ClockEvidence endEvidence;
    private ShiftReview review;

    private Shift(ShiftId id, SocietyId societyId, UserId staffId, Instant startedAt, Instant endedAt,
                  ClockEvidence startEvidence, ClockEvidence endEvidence, ShiftReview review) {
        this.id = Objects.requireNonNull(id, "id");
        this.societyId = Objects.requireNonNull(societyId, "societyId");
        this.staffId = Objects.requireNonNull(staffId, "staffId");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        if (endedAt != null && endedAt.isBefore(startedAt)) {
            throw new InvalidShiftException("A shift cannot end before it started");
        }
        if (review != null && endedAt == null) {
            throw new InvalidShiftException("Only an ended shift can be reviewed");
        }
        this.endedAt = endedAt;
        this.startEvidence = startEvidence;
        this.endEvidence = endEvidence;
        this.review = review;
    }

    /** The staff member clocks in with a fresh selfie of their own. */
    public static Shift start(ShiftId id, SocietyId societyId, UserId staffId, Instant at, ClockEvidence evidence) {
        requireOwnFreshSelfie(evidence, societyId, staffId, at);
        Shift shift = new Shift(id, societyId, staffId, at, null, evidence, null, null);
        shift.recordEvent(new ShiftStarted(id, societyId, staffId, at));
        return shift;
    }

    /** Rebuilds a stored shift; raises no events. */
    public static Shift reconstitute(ShiftId id, SocietyId societyId, UserId staffId, Instant startedAt,
                                     Instant endedAt, ClockEvidence startEvidence, ClockEvidence endEvidence,
                                     ShiftReview review) {
        return new Shift(id, societyId, staffId, startedAt, endedAt, startEvidence, endEvidence, review);
    }

    /** The staff member clocks out with a fresh selfie of their own. */
    public void end(Instant at, ClockEvidence evidence) {
        if (endedAt != null) {
            throw new InvalidShiftException("This shift has already ended");
        }
        if (at.isBefore(startedAt)) {
            throw new InvalidShiftException("A shift cannot end before it started");
        }
        requireOwnFreshSelfie(evidence, societyId, staffId, at);
        endedAt = at;
        endEvidence = evidence;
        recordEvent(new ShiftEnded(id, societyId, staffId, at));
    }

    /** An admin saw the staff member in both selfies and accepts where they were. A later review replaces this one. */
    public void confirm(UserId reviewer, Instant at) {
        recordReview(new ShiftReview(ShiftReview.Decision.CONFIRMED, reviewer, at, null));
        recordEvent(new ShiftConfirmed(id, societyId, staffId, reviewer, at));
    }

    /** An admin does not accept the shift as recorded, and says why. A later review replaces this one. */
    public void dispute(UserId reviewer, Instant at, String reason) {
        recordReview(new ShiftReview(ShiftReview.Decision.DISPUTED, reviewer, at, reason));
        recordEvent(new ShiftDisputed(id, societyId, staffId, reviewer, at));
    }

    private void recordReview(ShiftReview verdict) {
        if (endedAt == null) {
            throw new InvalidShiftException("Only an ended shift can be reviewed");
        }
        if (verdict.reviewer().equals(staffId)) {
            throw new InvalidShiftException("Nobody can review their own shift");
        }
        review = verdict;
    }

    /** Removes the selfies and locations (kept only for the retention period); times, checks and review stay. */
    public void forgetEvidence() {
        startEvidence = startEvidence == null ? null : startEvidence.forgotten();
        endEvidence = endEvidence == null ? null : endEvidence.forgotten();
    }

    /** Whether selfies or locations are still stored. */
    public boolean holdsPersonalEvidence() {
        return (startEvidence != null && !startEvidence.isForgotten())
                || (endEvidence != null && !endEvidence.isForgotten());
    }

    /** Clocked in or out outside the site, without a location, or without evidence at all. */
    public boolean needsAttention() {
        return startEvidence == null || !startEvidence.check().isInside()
                || (endedAt != null && (endEvidence == null || !endEvidence.check().isInside()));
    }

    private static void requireOwnFreshSelfie(ClockEvidence evidence, SocietyId societyId, UserId staffId,
                                              Instant at) {
        Objects.requireNonNull(evidence, "evidence");
        SelfieKey selfie = evidence.selfieKey()
                .orElseThrow(() -> new InvalidShiftException("Clocking in or out needs a selfie"));
        if (!selfie.belongsTo(societyId, staffId)) {
            throw new InvalidShiftException("That selfie belongs to someone else");
        }
        if (!selfie.isFreshAt(at)) {
            throw new InvalidShiftException("Take a new selfie: it must be taken just before clocking in or out");
        }
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

    public Optional<ClockEvidence> startEvidence() {
        return Optional.ofNullable(startEvidence);
    }

    public Optional<ClockEvidence> endEvidence() {
        return Optional.ofNullable(endEvidence);
    }

    public Optional<ShiftReview> review() {
        return Optional.ofNullable(review);
    }
}
