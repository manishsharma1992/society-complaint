package com.mycity.runwal.complaint.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** A complaint (or its input) breaks a business rule, e.g. a flat complaint without a flat. */
public class InvalidComplaintException extends DomainException {

    public InvalidComplaintException(String message) {
        super(message);
    }
}
