package com.mycity.runwal.residency.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** A Residency rule was broken (bad tower code, flat number, membership, ...). */
public class InvalidResidencyException extends DomainException {

    public InvalidResidencyException(String message) {
        super(message);
    }
}
