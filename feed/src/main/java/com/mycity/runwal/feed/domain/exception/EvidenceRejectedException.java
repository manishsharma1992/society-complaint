package com.mycity.runwal.feed.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** An uploaded file cannot be evidence on a post; the message tells the author why. */
public class EvidenceRejectedException extends DomainException {

    public EvidenceRejectedException(String message) {
        super(message);
    }
}
