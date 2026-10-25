package com.mycity.runwal.residency.domain.model.tower;

import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import java.util.regex.Pattern;

/** The short code residents use for a tower and in flat numbers, e.g. "B2" in "B2-503". */
@ValueObject
public record TowerCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9]{1,10}");

    public TowerCode {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidResidencyException("A tower code is 1 to 10 letters or digits, but was '" + value + "'");
        }
    }
}
