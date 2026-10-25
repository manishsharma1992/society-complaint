package com.mycity.runwal.identity.application.command;

/** Input to log in with the code that was sent. */
public record VerifyOtpCommand(String phone, String code) {
}
