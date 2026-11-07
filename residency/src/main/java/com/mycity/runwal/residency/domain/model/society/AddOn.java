package com.mycity.runwal.residency.domain.model.society;

import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import java.util.Arrays;

/** A part of the app a society can use or not. */
public enum AddOn {
    /** Residents' moderated evidence posts, from this society and nearby ones; the home page when on. */
    FEED,
    /** Complaints, with maintenance staff shifts and the staff report. */
    COMPLAINTS;

    public static AddOn from(String name) {
        return Arrays.stream(values()).filter(a -> a.name().equals(name)).findFirst()
                .orElseThrow(() -> new InvalidResidencyException(
                        "Unknown add-on '" + name + "'; expected one of " + Arrays.toString(values())));
    }
}
