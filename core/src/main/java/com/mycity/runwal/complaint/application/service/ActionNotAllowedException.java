package com.mycity.runwal.complaint.application.service;

/** The logged-in user may not do this (answered with 403). */
public class ActionNotAllowedException extends RuntimeException {

    public ActionNotAllowedException(String message) {
        super(message);
    }
}
