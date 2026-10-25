package com.mycity.runwal.shared.id;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;

/** Identity of a housing society, shared across bounded contexts. */
@ValueObject
public record SocietyId(long value) {

    public SocietyId {
        if (value <= 0) {
            throw new InvalidIdException("SocietyId", value);
        }
    }
}
