package com.mycity.runwal.complaint.domain.model.assignment;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Objects;
import java.util.Set;

/**
 * A staff member as automatic assignment sees them: who, and where they work.
 *
 * @param towers the towers they work in; empty when they work across the whole society
 * @param onCall a tower manager who passes the work on (e.g. to the lift company): needs no shift, takes any number
 *               of jobs, and only complaints raised in their own towers
 */
@ValueObject
public record StaffMember(UserId id, Set<TowerId> towers, boolean onCall) {

    public StaffMember {
        Objects.requireNonNull(id, "id");
        towers = Set.copyOf(towers);
    }

    /** Works where the complaint is: society-wide staff cover everything, tower staff their towers and shared areas. */
    public boolean covers(TowerId tower) {
        if (onCall && tower == null) {
            return false;
        }
        return towers.isEmpty() || tower == null || towers.contains(tower);
    }
}
