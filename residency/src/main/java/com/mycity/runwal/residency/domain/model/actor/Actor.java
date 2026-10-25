package com.mycity.runwal.residency.domain.model.actor;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Objects;
import java.util.Set;

/**
 * Whoever looks at or changes residency data, in this context's own terms: who they are, whether their account
 * is active, and what part they play in which society (translated from the login token).
 *
 * @param active false while a self sign-up is not yet approved
 */
@ValueObject
public record Actor(UserId userId, boolean active, Set<ActorRole> roles) {

    public Actor {
        Objects.requireNonNull(userId, "userId");
        roles = Set.copyOf(roles);
    }

    public boolean is(UserId user) {
        return userId.equals(user);
    }

    /** Society admin of the society, or tower admin of this tower (null tower: society-wide, admins only). */
    public boolean manages(SocietyId societyId, TowerId towerId) {
        return has(ActorRole.Part.SOCIETY_ADMIN, societyId)
                || (towerId != null && roles.contains(new ActorRole(ActorRole.Part.TOWER_ADMIN, societyId, towerId)));
    }

    /** The society admin or any tower admin of the society. */
    public boolean isAdminIn(SocietyId societyId) {
        return has(ActorRole.Part.SOCIETY_ADMIN, societyId) || has(ActorRole.Part.TOWER_ADMIN, societyId);
    }

    private boolean has(ActorRole.Part part, SocietyId societyId) {
        return roles.stream().anyMatch(role -> role.part() == part && role.societyId().equals(societyId));
    }
}
