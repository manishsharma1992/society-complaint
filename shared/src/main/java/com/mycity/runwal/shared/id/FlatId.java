package com.mycity.runwal.shared.id;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;

/** Identity of a flat within a tower, shared across bounded contexts. */
@ValueObject
public record FlatId(long value) {

    public FlatId {
        if (value <= 0) {
            throw new InvalidIdException("FlatId", value);
        }
    }
}
