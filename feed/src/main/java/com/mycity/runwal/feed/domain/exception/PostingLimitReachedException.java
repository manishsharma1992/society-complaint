package com.mycity.runwal.feed.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** The author has already submitted the most posts allowed in one day. Says nothing about who the author is. */
public class PostingLimitReachedException extends DomainException {

    public PostingLimitReachedException(int limit) {
        super("You can submit at most " + limit + " posts a day. Please try again tomorrow.");
    }
}
