package com.mycity.runwal.workforce.domain.model.shift;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import java.util.Objects;
import java.util.Optional;

/**
 * What a staff member gave when clocking in or out: a live selfie and, if the phone allowed it, where they were.
 * After the retention period the selfie and the location are forgotten; the site check stays.
 *
 * @param selfie   null once forgotten
 * @param location null when the phone gave none, or once forgotten
 */
@ValueObject
public record ClockEvidence(SelfieKey selfie, Location location, SiteCheck check) {

    public ClockEvidence {
        Objects.requireNonNull(check, "check");
    }

    public static ClockEvidence at(Site site, SelfieKey selfie, Location location) {
        return new ClockEvidence(Objects.requireNonNull(selfie, "selfie"), location, site.check(location));
    }

    public Optional<SelfieKey> selfieKey() {
        return Optional.ofNullable(selfie);
    }

    public Optional<Location> where() {
        return Optional.ofNullable(location);
    }

    public boolean isForgotten() {
        return selfie == null;
    }

    /** The selfie and the location removed (personal data kept only as long as needed); the check result stays. */
    public ClockEvidence forgotten() {
        return new ClockEvidence(null, null, check);
    }
}
