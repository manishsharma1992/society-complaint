package com.mycity.runwal.identity.application.service;

/** The code is wrong, expired, already used, or too many wrong codes were tried. Deliberately vague. */
public class LoginFailedException extends RuntimeException {

    public LoginFailedException() {
        super("The code is wrong or has expired. Request a new code and try again.");
    }
}
