package com.mycity.runwal.workforce.domain.model.actor;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Objects;
import java.util.Set;

/**
 * A logged-in user, in this context's terms (translated from the login token).
 *
 * @param active false while a self sign-up is not yet approved
 */
@ValueObject
public record Actor(UserId userId, boolean active, Set<ActorRole> roles) {

    public Actor {
        Objects.requireNonNull(userId, "userId");
        roles = Set.copyOf(roles);
    }

    public boolean has(ActorRole.Part part, SocietyId societyId) {
        return roles.contains(new ActorRole(part, societyId));
    }
}
