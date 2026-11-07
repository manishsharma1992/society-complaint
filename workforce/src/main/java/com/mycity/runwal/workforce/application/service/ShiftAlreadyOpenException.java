package com.mycity.runwal.workforce.application.service;

/** The staff member tried to clock in while already on a shift (answered with 409). */
public class ShiftAlreadyOpenException extends RuntimeException {

    public ShiftAlreadyOpenException() {
        super("You are already on a shift; end it first");
    }
}
