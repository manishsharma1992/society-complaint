package com.mycity.runwal.workforce.application.command;

/**
 * The logged-in staff member clocks in with the selfie just uploaded and, if the phone gave one, their location.
 *
 * @param latitude null (with longitude and accuracy) when the phone gave no location
 */
public record StartShiftCommand(long societyId, String selfieKey, Double latitude, Double longitude,
                                Double accuracyMeters) {
}
