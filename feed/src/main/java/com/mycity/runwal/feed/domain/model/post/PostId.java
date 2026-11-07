package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;

@ValueObject
public record PostId(long value) {

    public PostId {
        if (value <= 0) {
            throw new InvalidPostException("PostId must be positive, but was " + value);
        }
    }
}
