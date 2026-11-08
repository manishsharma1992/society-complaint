package com.mycity.runwal.residency.application.query;

/** Whether the add-on is on for the society and not switched off for this tower: other contexts ask before acting. */
public record AddOnOnInTowerQuery(long societyId, String addOn, long towerId) {
}
