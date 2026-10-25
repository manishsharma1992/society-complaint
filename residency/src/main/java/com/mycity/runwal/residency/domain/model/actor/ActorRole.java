package com.mycity.runwal.residency.domain.model.actor;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import java.util.Objects;

/**
 * The part someone plays in a society.
 *
 * @param towerId the tower a tower admin (or tower staff) looks after; null when society-wide
 */
@ValueObject
public record ActorRole(Part part, SocietyId societyId, TowerId towerId) {

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
