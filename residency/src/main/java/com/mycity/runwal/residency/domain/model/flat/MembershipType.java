package com.mycity.runwal.residency.domain.model.flat;

import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import java.util.Arrays;

/** How a person is linked to a flat. */
public enum MembershipType {
    OWNER,
    TENANT;

    public static MembershipType from(String name) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new InvalidResidencyException(
                        "Unknown membership type '" + name + "'; expected one of " + Arrays.toString(values())));
    }
}
