package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import java.util.Arrays;

/** Whether a complaint is about one flat or about a common area (lift, garden, parking, ...). */
public enum ComplaintScope {
    FLAT,
    COMMON_AREA;

    public static ComplaintScope from(String name) {
        return Arrays.stream(values())
                .filter(scope -> scope.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new InvalidComplaintException(
                        "Unknown complaint scope '" + name + "'; expected one of " + Arrays.toString(values())));
    }
}
