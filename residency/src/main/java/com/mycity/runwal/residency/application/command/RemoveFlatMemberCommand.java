package com.mycity.runwal.residency.application.command;

/** Input to unlink a person from a flat (e.g. a tenant moving out). */
public record RemoveFlatMemberCommand(long societyId, long flatId, long userId) {
}
