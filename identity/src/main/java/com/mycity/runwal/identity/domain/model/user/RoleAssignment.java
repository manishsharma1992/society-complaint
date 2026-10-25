package com.mycity.runwal.identity.domain.model.user;

import com.mycity.runwal.identity.domain.exception.InvalidIdentityException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import java.util.Objects;

/**
 * A role in one society, optionally limited to one tower. A role without a tower is society-wide.
 *
 * @param towerId required for TOWER_ADMIN, optional for STAFF, not allowed for RESIDENT and SOCIETY_ADMIN
 */
@ValueObject
public record RoleAssignment(Role role, SocietyId societyId, TowerId towerId) {

    public RoleAssignment {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(societyId, "societyId");
        if (role == Role.TOWER_ADMIN && towerId == null) {
            throw new InvalidIdentityException("A tower admin needs a tower");
        }
        if ((role == Role.RESIDENT || role == Role.SOCIETY_ADMIN) && towerId != null) {
            throw new InvalidIdentityException("The " + role + " role is society-wide and takes no tower");
        }
    }

    public static RoleAssignment societyWide(Role role, SocietyId societyId) {
        return new RoleAssignment(role, societyId, null);
    }
}
