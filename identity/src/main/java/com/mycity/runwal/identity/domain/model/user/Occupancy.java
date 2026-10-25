package com.mycity.runwal.identity.domain.model.user;

import com.mycity.runwal.identity.domain.exception.InvalidIdentityException;
import java.util.Arrays;

/** How a registering resident says they live in the flat. */
public enum Occupancy {
    OWNER,
    TENANT;

    public static Occupancy from(String name) {
        return Arrays.stream(values())
                .filter(occupancy -> occupancy.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new InvalidIdentityException(
                        "Unknown occupancy '" + name + "'; expected one of " + Arrays.toString(values())));
    }
}
