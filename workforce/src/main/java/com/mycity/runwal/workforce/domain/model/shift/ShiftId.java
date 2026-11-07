package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;

@ValueObject
public record ShiftId(long value) {

    public ShiftId {
        if (value <= 0) {
            throw new InvalidShiftException("ShiftId must be positive, but was " + value);
        }
    }
}
