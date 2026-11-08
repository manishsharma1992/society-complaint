package com.mycity.runwal.complaint.domain.model.assignment;

import com.mycity.runwal.complaint.domain.event.AutoAssignmentSwitched;
import com.mycity.runwal.shared.ddd.AbstractAggregateRoot;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Instant;
import java.util.Objects;

/**
 * Whether a society's complaints are assigned automatically: the most urgent waiting complaint goes to a free staff
 * member on shift who handles its category. Admins can always assign or reassign by hand as well.
 */
@AggregateRoot
public class AutoAssignment extends AbstractAggregateRoot {

    private final SocietyId societyId;
    private boolean on;

    public AutoAssignment(SocietyId societyId, boolean on) {
        this.societyId = Objects.requireNonNull(societyId, "societyId");
        this.on = on;
    }

    /** Societies that never chose start with automatic assignment off. */
    public static AutoAssignment offFor(SocietyId societyId) {
        return new AutoAssignment(societyId, false);
    }

    public void switchOn(Instant at) {
        if (!on) {
            on = true;
            recordEvent(new AutoAssignmentSwitched(societyId, true, at));
        }
    }

    public void switchOff(Instant at) {
        if (on) {
            on = false;
            recordEvent(new AutoAssignmentSwitched(societyId, false, at));
        }
    }

    public SocietyId societyId() {
        return societyId;
    }

    public boolean isOn() {
        return on;
    }
}
