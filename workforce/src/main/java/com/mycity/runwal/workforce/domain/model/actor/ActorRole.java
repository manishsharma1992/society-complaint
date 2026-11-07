package com.mycity.runwal.workforce.domain.model.actor;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.Objects;

/** The part someone plays in a society, as far as staff shifts are concerned. */
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
