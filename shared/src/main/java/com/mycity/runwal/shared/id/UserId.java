package com.mycity.runwal.shared.id;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;

/** Identity of an app user, shared across bounded contexts. */
@ValueObject
public record UserId(long value) {

    public UserId {
        if (value <= 0) {
            throw new InvalidIdException("UserId", value);
        }
    }
}
