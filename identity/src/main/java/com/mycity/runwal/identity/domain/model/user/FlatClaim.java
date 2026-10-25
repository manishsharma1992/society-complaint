package com.mycity.runwal.identity.domain.model.user;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.Objects;

/** The flat a resident says they live in when registering; checked by the society before approval. */
@ValueObject
public record FlatClaim(SocietyId societyId, FlatId flatId, Occupancy occupancy) {

    public FlatClaim {
        Objects.requireNonNull(societyId, "societyId");
        Objects.requireNonNull(flatId, "flatId");
        Objects.requireNonNull(occupancy, "occupancy");
    }
}
