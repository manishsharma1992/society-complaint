package com.mycity.runwal.residency.domain.model.flat;

import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.residency.domain.model.tower.TowerCode;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;

/**
 * A flat's number within its tower: floor 5, unit 3 is "503". Floors start at the first residential floor.
 *
 * @param unit the flat's position on its floor, 1 to 99
 */
@ValueObject
public record FlatNumber(int floor, int unit) {

    public FlatNumber {
        if (floor < 1 || floor > 99 || unit < 1 || unit > 99) {
            throw new InvalidResidencyException("A flat number needs floor and unit between 1 and 99");
        }
    }

    /** The number as residents say it, e.g. 503. */
    public int value() {
        return floor * 100 + unit;
    }

    /** The full label residents use, e.g. "B2-503". */
    public String label(TowerCode tower) {
        return tower.value() + "-" + value();
    }
}
