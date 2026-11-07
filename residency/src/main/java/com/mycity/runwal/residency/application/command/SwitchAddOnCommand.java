package com.mycity.runwal.residency.application.command;

/** The society admin switches an available add-on on or off for their society. */
public record SwitchAddOnCommand(long societyId, String addOn, boolean on) {
}
