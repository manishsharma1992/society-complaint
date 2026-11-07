package com.mycity.runwal.workforce.application.service;

/** The staff member tried to clock out without being clocked in (answered with 404). */
public class NoOpenShiftException extends RuntimeException {

    public NoOpenShiftException() {
        super("You are not on a shift");
    }
}
