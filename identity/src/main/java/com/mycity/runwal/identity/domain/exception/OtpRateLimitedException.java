package com.mycity.runwal.identity.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** Codes were requested too often for this phone number. */
public class OtpRateLimitedException extends DomainException {

    public OtpRateLimitedException(String message) {
        super(message);
    }
}
