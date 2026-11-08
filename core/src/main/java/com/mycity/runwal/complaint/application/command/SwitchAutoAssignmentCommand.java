package com.mycity.runwal.complaint.application.command;

/** The society admin turns automatic assignment of complaints on or off. */
public record SwitchAutoAssignmentCommand(long societyId, boolean on) {
}
