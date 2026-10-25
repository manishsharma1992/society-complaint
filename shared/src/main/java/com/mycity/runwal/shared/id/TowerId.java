package com.mycity.runwal.shared.id;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;

/** Identity of a tower within a society, shared across bounded contexts. */
@ValueObject
public record TowerId(long value) {

    public TowerId {
        if (value <= 0) {
            throw new InvalidIdException("TowerId", value);
        }
    }
}
