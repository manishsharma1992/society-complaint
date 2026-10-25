package com.mycity.runwal.complaint.domain.exception;

import com.mycity.runwal.shared.ddd.DomainException;

/** A photo is being attached, but object storage has nothing under its key (yet). */
public class PhotoNotUploadedException extends DomainException {

    public PhotoNotUploadedException(String storageKey) {
        super("No photo has been uploaded under " + storageKey);
    }
}
