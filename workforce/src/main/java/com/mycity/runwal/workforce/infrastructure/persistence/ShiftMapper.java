package com.mycity.runwal.workforce.infrastructure.persistence;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.model.shift.ClockEvidence;
import com.mycity.runwal.workforce.domain.model.shift.GeoPoint;
import com.mycity.runwal.workforce.domain.model.shift.Location;
import com.mycity.runwal.workforce.domain.model.shift.Placement;
import com.mycity.runwal.workforce.domain.model.shift.SelfieKey;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.ShiftId;
import com.mycity.runwal.workforce.domain.model.shift.ShiftReview;
import com.mycity.runwal.workforce.domain.model.shift.SiteCheck;

/** Maps the Shift aggregate to its one table row and back; clock evidence is flattened into start_* and end_*. */
final class ShiftMapper {

    private ShiftMapper() {
    }

    static ShiftJpaEntity toEntity(Shift shift) {
        ShiftJpaEntity e = new ShiftJpaEntity();
        e.id = shift.id().value();
        e.societyId = shift.societyId().value();
        e.staffId = shift.staffId().value();
        e.startedAt = shift.startedAt();
        e.endedAt = shift.endedAt().orElse(null);
        shift.startEvidence().ifPresent(ev -> {
            e.startSelfieKey = ev.selfieKey().map(SelfieKey::value).orElse(null);
            e.startLatitude = ev.where().map(l -> l.point().latitude()).orElse(null);
            e.startLongitude = ev.where().map(l -> l.point().longitude()).orElse(null);
            e.startAccuracyMeters = ev.where().map(Location::accuracyMeters).orElse(null);
            e.startPlacement = ev.check().placement().name();
            e.startDistanceMeters = ev.check().distanceMeters();
        });
        shift.endEvidence().ifPresent(ev -> {
            e.endSelfieKey = ev.selfieKey().map(SelfieKey::value).orElse(null);
            e.endLatitude = ev.where().map(l -> l.point().latitude()).orElse(null);
            e.endLongitude = ev.where().map(l -> l.point().longitude()).orElse(null);
            e.endAccuracyMeters = ev.where().map(Location::accuracyMeters).orElse(null);
            e.endPlacement = ev.check().placement().name();
            e.endDistanceMeters = ev.check().distanceMeters();
        });
        shift.review().ifPresent(r -> {
            e.reviewDecision = r.decision().name();
            e.reviewerId = r.reviewer().value();
            e.reviewedAt = r.reviewedAt();
            e.reviewReason = r.reason();
        });
        return e;
    }

    static Shift toDomain(ShiftJpaEntity e) {
        ClockEvidence start = evidence(e.startPlacement, e.startDistanceMeters, e.startSelfieKey, e.startLatitude,
                e.startLongitude, e.startAccuracyMeters);
        ClockEvidence end = evidence(e.endPlacement, e.endDistanceMeters, e.endSelfieKey, e.endLatitude,
                e.endLongitude, e.endAccuracyMeters);
        ShiftReview review = e.reviewDecision == null ? null : new ShiftReview(
                ShiftReview.Decision.valueOf(e.reviewDecision), new UserId(e.reviewerId), e.reviewedAt, e.reviewReason);
        return Shift.reconstitute(new ShiftId(e.id), new SocietyId(e.societyId), new UserId(e.staffId), e.startedAt,
                e.endedAt, start, end, review);
    }

    /** No placement means the clock event has no evidence (shifts recorded before it was required). */
    private static ClockEvidence evidence(String placement, Long distance, String selfieKey, Double latitude,
                                          Double longitude, Double accuracy) {
        if (placement == null) {
            return null;
        }
        Location location = latitude == null ? null : new Location(new GeoPoint(latitude, longitude), accuracy);
        return new ClockEvidence(selfieKey == null ? null : new SelfieKey(selfieKey), location,
                new SiteCheck(Placement.valueOf(placement), distance));
    }
}
