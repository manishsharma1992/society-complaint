package com.mycity.runwal.identity.application.service;

/** Codes were requested too often for this phone number; the message says how long to wait. */
public class TooManyCodesException extends RuntimeException {

    public TooManyCodesException(String message) {
        super(message);
    }
}
