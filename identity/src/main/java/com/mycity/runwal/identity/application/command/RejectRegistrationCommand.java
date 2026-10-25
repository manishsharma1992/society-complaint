package com.mycity.runwal.identity.application.command;

/** Input to turn down a pending registration, with a reason the user sees. */
public record RejectRegistrationCommand(long societyId, long userId, String reason) {
}
