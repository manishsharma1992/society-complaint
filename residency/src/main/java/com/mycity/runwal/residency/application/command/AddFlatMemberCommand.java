package com.mycity.runwal.residency.application.command;

/**
 * Input to link a person to a flat. The caller comes from the login token.
 *
 * @param type "OWNER" or "TENANT"
 */
public record AddFlatMemberCommand(long societyId, long flatId, long userId, String type) {
}
