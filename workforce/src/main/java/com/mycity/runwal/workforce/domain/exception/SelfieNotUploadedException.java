package com.mycity.runwal.workforce.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** Clocking in or out named a selfie that was never uploaded. */
public class SelfieNotUploadedException extends DomainException {

    public SelfieNotUploadedException(String storageKey) {
        super("No selfie was uploaded under " + storageKey + "; take the selfie again");
    }
}
