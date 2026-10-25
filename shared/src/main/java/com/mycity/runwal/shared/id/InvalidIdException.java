package com.mycity.runwal.shared.id;

import com.mycity.runwal.shared.ddd.DomainException;

/** Thrown when a cross-context ID is created from an invalid value. */
public final class InvalidIdException extends DomainException {

    InvalidIdException(String idType, long value) {
        super(idType + " must be positive, but was " + value);
    }
}
