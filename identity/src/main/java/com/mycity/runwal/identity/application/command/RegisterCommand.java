package com.mycity.runwal.identity.application.command;

/** Input for the logged-in (verified) user to register as resident of a flat; occupancy is OWNER or TENANT. */
public record RegisterCommand(String name, long societyId, long flatId, String occupancy) {
}
