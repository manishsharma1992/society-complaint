package com.mycity.runwal.complaint.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** A complaint category breaks a business rule, e.g. a non-positive SLA. */
public class InvalidComplaintCategoryException extends DomainException {

    public InvalidComplaintCategoryException(String message) {
        super(message);
    }
}
