package com.mycity.runwal.residency.application.command;

/** The society admin switches an add-on (only COMPLAINTS) off for a tower that handles it its own way, or back on. */
public record SwitchAddOnInTowerCommand(long societyId, String addOn, long towerId, boolean on) {
}
