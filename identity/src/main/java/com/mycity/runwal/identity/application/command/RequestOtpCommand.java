package com.mycity.runwal.identity.application.command;

/** Input to send a one-time code to a phone number. */
public record RequestOtpCommand(String phone) {
}
