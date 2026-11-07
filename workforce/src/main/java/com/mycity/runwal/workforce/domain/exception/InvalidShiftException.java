package com.mycity.runwal.workforce.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** A shift breaks a rule of the workforce context, e.g. it is ended twice or before it started. */
public class InvalidShiftException extends DomainException {

    public InvalidShiftException(String message) {
        super(message);
    }
}
