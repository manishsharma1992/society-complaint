package com.mycity.runwal.identity.application.command;

/** Input to take a role away from a user. */
public record RevokeRoleCommand(long societyId, long userId, String role, Long towerId) {
}
