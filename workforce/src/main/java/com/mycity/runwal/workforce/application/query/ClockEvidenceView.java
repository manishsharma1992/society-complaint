package com.mycity.runwal.workforce.application.query;

import com.mycity.runwal.workforce.application.port.SelfieStorage;
import com.mycity.runwal.workforce.domain.model.shift.ClockEvidence;
import java.time.Duration;

/**
 * A clock-in or clock-out as admins and the staff member see it.
 *
 * @param placement   INSIDE, OUTSIDE or NO_LOCATION
 * @param selfieUrl   a short-lived link, or null once the selfie was deleted after the retention period
 * @param latitude    null without a location or once forgotten
 */
public record ClockEvidenceView(String placement, Long distanceMeters, Double latitude, Double longitude,
                                Double accuracyMeters, String selfieUrl) {

    static final Duration SELFIE_LINK_VALID_FOR = Duration.ofMinutes(15);

    public static ClockEvidenceView of(ClockEvidence evidence, SelfieStorage storage) {
        return new ClockEvidenceView(evidence.check().placement().name(), evidence.check().distanceMeters(),
                evidence.where().map(l -> l.point().latitude()).orElse(null),
                evidence.where().map(l -> l.point().longitude()).orElse(null),
                evidence.where().map(l -> l.accuracyMeters()).orElse(null),
                evidence.selfieKey().map(k -> storage.viewUrl(k.value(), SELFIE_LINK_VALID_FOR).toString()).orElse(null));
    }
}
