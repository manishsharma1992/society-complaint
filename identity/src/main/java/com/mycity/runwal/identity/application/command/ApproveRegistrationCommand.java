package com.mycity.runwal.identity.application.command;

/** Input to accept a pending registration. The deciding admin comes from the login token. */
public record ApproveRegistrationCommand(long societyId, long userId) {
}
