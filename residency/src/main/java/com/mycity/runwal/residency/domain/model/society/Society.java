package com.mycity.runwal.residency.domain.model.society;

import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.Objects;

/** A housing complex, e.g. "Runwal MyCity". Everything in the app belongs to exactly one society. */
@AggregateRoot
public class Society {

    public static final int MAX_NAME_LENGTH = 150;

    private final SocietyId id;
    private final String name;

    public Society(SocietyId id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH) {
            throw new InvalidResidencyException("A society needs a name of at most " + MAX_NAME_LENGTH + " characters");
        }
        this.name = name.strip();
    }

    public SocietyId id() {
        return id;
    }

    public String name() {
        return name;
    }
}
