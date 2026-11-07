package com.mycity.runwal.feed.domain.model.actor;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.Objects;

/** The part someone plays in a society, as far as the public feed cares. */
@ValueObject
public record ActorRole(Part part, SocietyId societyId) {

    public enum Part {
        RESIDENT,
        STAFF,
        TOWER_ADMIN,
        SOCIETY_ADMIN
    }

    public ActorRole {
        Objects.requireNonNull(part, "part");
        Objects.requireNonNull(societyId, "societyId");
    }
}
