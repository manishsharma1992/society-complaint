package com.mycity.runwal.identity.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** An Identity rule was broken (bad phone number, registration in the wrong state, invalid role, ...). */
public class InvalidIdentityException extends DomainException {

    public InvalidIdentityException(String message) {
        super(message);
    }
}
