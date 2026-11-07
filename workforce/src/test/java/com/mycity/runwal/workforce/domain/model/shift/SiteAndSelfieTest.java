package com.mycity.runwal.workforce.domain.model.shift;

import static com.mycity.runwal.workforce.domain.model.shift.ShiftTest.RAJU;
import static com.mycity.runwal.workforce.domain.model.shift.ShiftTest.RUNWAL;
import static com.mycity.runwal.workforce.domain.model.shift.ShiftTest.SITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SiteAndSelfieTest {

    @Test
    void distancesAreGreatCircleMetres() {
        // one thousandth of a degree of latitude is about 111 m everywhere
        GeoPoint a = new GeoPoint(19.186, 73.0748);
        assertThat(a.metersTo(new GeoPoint(19.187, 73.0748))).isCloseTo(111.2, within(0.5));
        assertThat(a.metersTo(a)).isZero();
    }

    @Test
    void theSiteSaysInsideOutsideOrNoLocation() {
        SiteCheck gate = SITE.check(new Location(new GeoPoint(19.1865, 73.0750), 12));
        SiteCheck edge = SITE.check(new Location(new GeoPoint(19.186062 + 0.0026, 73.074812), 5)); // ~289 m north
        SiteCheck justOut = SITE.check(new Location(new GeoPoint(19.186062 + 0.0028, 73.074812), 5)); // ~311 m
        SiteCheck station = SITE.check(new Location(new GeoPoint(19.2183, 73.0868), 20));

        assertThat(gate.placement()).isEqualTo(Placement.INSIDE);
        assertThat(gate.distanceMeters()).isLessThan(60);
        assertThat(edge.placement()).isEqualTo(Placement.INSIDE);
        assertThat(justOut.placement()).isEqualTo(Placement.OUTSIDE);
        assertThat(station.placement()).isEqualTo(Placement.OUTSIDE);
        assertThat(station.distanceMeters()).isBetween(3_500L, 4_000L);
        assertThat(SITE.check(null)).isEqualTo(SiteCheck.noLocation());
    }

    @Test
    void impossibleLocationsAndSitesAreRefused() {
        assertThatThrownBy(() -> new GeoPoint(91, 0)).isInstanceOf(InvalidShiftException.class);
        assertThatThrownBy(() -> new GeoPoint(0, Double.NaN)).isInstanceOf(InvalidShiftException.class);
        assertThatThrownBy(() -> new Location(new GeoPoint(0, 0), -1)).isInstanceOf(InvalidShiftException.class);
        assertThatThrownBy(() -> new Site(new GeoPoint(0, 0), 0)).isInstanceOf(InvalidShiftException.class);
    }

    @Test
    void aSelfieKeyNamesItsOwnerAndWhenItsLinkWasIssued() {
        Instant issued = Instant.parse("2026-11-07T00:29:30Z");
        SelfieKey key = SelfieKey.issue(RUNWAL, RAJU, issued, "0b6f1c2e-6a1d-4c5e-9f0e-2d7a3c4b5e6f");

        assertThat(key.value()).startsWith("shifts/1/30/" + issued.toEpochMilli() + "-").endsWith(".jpg");
        assertThat(key.belongsTo(RUNWAL, RAJU)).isTrue();
        assertThat(key.belongsTo(RUNWAL, new UserId(31))).isFalse();
        assertThat(key.belongsTo(new SocietyId(2), RAJU)).isFalse();
        assertThat(key.issuedAt()).isEqualTo(issued);
        assertThat(key.isFreshAt(issued.plus(Duration.ofMinutes(10)))).isTrue();
        assertThat(key.isFreshAt(issued.plus(Duration.ofMinutes(11)))).isFalse();
        assertThat(key.isFreshAt(issued.minusSeconds(1))).isFalse();
    }

    @Test
    void onlyKeysInTheSelfieFormatAreAccepted() {
        for (String bad : new String[] {"complaints/1/7/a.jpg", "shifts/1/30/abc-12345678.jpg",
                "shifts/1/30/1700000000000-12345678.png", "shifts/1/30/1700000000000-../../x.jpg", ""}) {
            assertThatThrownBy(() -> new SelfieKey(bad)).isInstanceOf(InvalidShiftException.class);
        }
    }

    @Test
    void aReviewDisputeNeedsAReasonOfAtMost500Characters() {
        Instant at = Instant.parse("2026-11-07T10:00:00Z");
        assertThatThrownBy(() -> new ShiftReview(ShiftReview.Decision.DISPUTED, new UserId(2), at, null))
                .isInstanceOf(InvalidShiftException.class);
        assertThatThrownBy(() -> new ShiftReview(ShiftReview.Decision.DISPUTED, new UserId(2), at, "x".repeat(501)))
                .isInstanceOf(InvalidShiftException.class);
        assertThat(new ShiftReview(ShiftReview.Decision.CONFIRMED, new UserId(2), at, " ").reason()).isNull();
    }
}
