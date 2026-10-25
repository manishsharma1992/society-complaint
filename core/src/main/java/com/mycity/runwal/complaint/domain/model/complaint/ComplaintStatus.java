package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import java.util.Arrays;

/**
 * Lifecycle of a complaint: OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED.
 * An OPEN or ASSIGNED complaint can be REJECTED. CLOSED and REJECTED are final.
 */
public enum ComplaintStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    REJECTED;

    public boolean canMoveTo(ComplaintStatus target) {
        return switch (this) {
            case OPEN -> target == ASSIGNED || target == REJECTED;
            case ASSIGNED -> target == IN_PROGRESS || target == REJECTED;
            case IN_PROGRESS -> target == RESOLVED;
            case RESOLVED -> target == CLOSED;
            case CLOSED, REJECTED -> false;
        };
    }

    public static ComplaintStatus from(String name) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new InvalidComplaintException(
                        "Unknown complaint status '" + name + "'; expected one of " + Arrays.toString(values())));
    }
}
