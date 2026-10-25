package com.mycity.runwal.shared.ddd;

/**
 * Base class for every exception thrown by a domain model.
 * Each context defines specific subclasses that name the broken rule,
 * e.g. {@code InvalidStatusTransitionException}.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
