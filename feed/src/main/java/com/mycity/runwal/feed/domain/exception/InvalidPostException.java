package com.mycity.runwal.feed.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** A post, report or moderation decision breaks a rule of the public feed. */
public class InvalidPostException extends DomainException {

    public InvalidPostException(String message) {
        super(message);
    }
}
