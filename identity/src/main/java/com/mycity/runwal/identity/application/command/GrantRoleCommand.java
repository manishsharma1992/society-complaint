package com.mycity.runwal.identity.application.command;

/** Input to give a user a role; towerId is null for a society-wide role. */
public record GrantRoleCommand(long societyId, long userId, String role, Long towerId) {
}
