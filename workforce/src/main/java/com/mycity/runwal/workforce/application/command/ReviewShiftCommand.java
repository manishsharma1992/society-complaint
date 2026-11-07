package com.mycity.runwal.workforce.application.command;

/**
 * An admin confirms or disputes an ended shift after checking its selfies and locations.
 *
 * @param reason required to dispute
 */
public record ReviewShiftCommand(long societyId, long shiftId, Decision decision, String reason) {

    public enum Decision {
        CONFIRM,
        DISPUTE
    }
}
