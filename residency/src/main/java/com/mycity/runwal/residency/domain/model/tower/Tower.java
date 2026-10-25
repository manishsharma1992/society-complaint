package com.mycity.runwal.residency.domain.model.tower;

import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import java.util.Objects;

/**
 * A building of a society, e.g. code "B2", name "Radiant". Floors are counted from the first residential floor,
 * so parking levels are not included.
 */
@AggregateRoot
public class Tower {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_FLOORS = 99;

    private final TowerId id;
    private final SocietyId societyId;
    private final TowerCode code;
    private final String name;
    private final int residentialFloors;

    public Tower(TowerId id, SocietyId societyId, TowerCode code, String name, int residentialFloors) {
        this.id = Objects.requireNonNull(id, "id");
        this.societyId = Objects.requireNonNull(societyId, "societyId");
        this.code = Objects.requireNonNull(code, "code");
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH) {
            throw new InvalidResidencyException("A tower needs a name of at most " + MAX_NAME_LENGTH + " characters");
        }
        if (residentialFloors < 1 || residentialFloors > MAX_FLOORS) {
            throw new InvalidResidencyException("A tower has 1 to " + MAX_FLOORS + " residential floors");
        }
        this.name = name.strip();
        this.residentialFloors = residentialFloors;
    }

    public TowerId id() {
        return id;
    }

    public SocietyId societyId() {
        return societyId;
    }

    public TowerCode code() {
        return code;
    }

    public String name() {
        return name;
    }

    public int residentialFloors() {
        return residentialFloors;
    }
}
