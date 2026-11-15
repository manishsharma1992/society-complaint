package com.mycity.runwal.complaint.application.service;

/** The translation service could not be used right now (answered with 503). */
public class TranslationUnavailableException extends RuntimeException {

    public TranslationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
