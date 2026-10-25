package com.mycity.runwal.identity.domain.model.user;

import com.mycity.runwal.identity.domain.exception.InvalidIdentityException;
import java.util.Arrays;

/** What a user may do in a society; see {@link RoleAssignment} for the scope. */
public enum Role {
    /** Lives in the society (linked to a flat in Residency); raises and follows complaints. */
    RESIDENT,
    /** Fixes complaints (plumber, electrician, housekeeping, ...); society-wide or for one tower. */
    STAFF,
    /** Manages complaints of one tower. */
    TOWER_ADMIN,
    /** Manages the whole society: complaints, registrations, roles. */
    SOCIETY_ADMIN;

    public static Role from(String name) {
        return Arrays.stream(values())
                .filter(role -> role.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new InvalidIdentityException(
                        "Unknown role '" + name + "'; expected one of " + Arrays.toString(values())));
    }
}
