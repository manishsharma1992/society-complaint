package com.mycity.runwal.identity.application.query;

import com.mycity.runwal.identity.domain.model.user.RoleAssignment;

/** @param towerId null for a society-wide role */
public record RoleView(String role, long societyId, Long towerId) {

    static RoleView of(RoleAssignment role) {
        return new RoleView(role.role().name(), role.societyId().value(),
                role.towerId() == null ? null : role.towerId().value());
    }
}
