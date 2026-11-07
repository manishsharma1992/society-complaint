package com.mycity.runwal.workforce.application.command;

/**
 * The logged-in staff member clocks out of their open shift with the selfie just uploaded and their location.
 *
 * @param latitude null (with longitude and accuracy) when the phone gave no location
 */
public record EndShiftCommand(long societyId, String selfieKey, Double latitude, Double longitude,
                              Double accuracyMeters) {
}
