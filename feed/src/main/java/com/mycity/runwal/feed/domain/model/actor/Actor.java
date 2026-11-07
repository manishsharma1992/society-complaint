package com.mycity.runwal.feed.domain.model.actor;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Objects;
import java.util.Set;

/**
 * A logged-in user acting on the feed, in this context's terms (translated from the login token).
 *
 * @param active false while a self sign-up is not yet approved
 */
@ValueObject
public record Actor(UserId userId, boolean active, Set<ActorRole> roles) {

    public Actor {
        Objects.requireNonNull(userId, "userId");
        roles = Set.copyOf(roles);
    }

    /** Any part at all in the society (resident, staff or admin). */
    public boolean belongsTo(SocietyId societyId) {
        return roles.stream().anyMatch(role -> role.societyId().equals(societyId));
    }

    public boolean has(ActorRole.Part part, SocietyId societyId) {
        return roles.contains(new ActorRole(part, societyId));
    }
}
