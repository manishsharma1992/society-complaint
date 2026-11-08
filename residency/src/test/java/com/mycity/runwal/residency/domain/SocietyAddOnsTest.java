package com.mycity.runwal.residency.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.residency.domain.model.society.AddOn;
import com.mycity.runwal.residency.domain.model.society.GeoPoint;
import com.mycity.runwal.residency.domain.model.society.Society;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SocietyAddOnsTest {

    private static final GeoPoint RUNWAL = new GeoPoint(19.186062, 73.074812);
    /** Palava, about 6 km south-west of Runwal MyCity */
    private static final GeoPoint PALAVA = new GeoPoint(19.1580, 73.0300);
    /** Thane, about 20 km away */
    private static final GeoPoint THANE = new GeoPoint(19.2183, 72.9781);

    @Test
    void aSocietyUsesOnlyAddOnsThePlatformMadeAvailableAndStartsWithThemOff() {
        Society society = new Society(new SocietyId(1), "Runwal MyCity");
        assertThatThrownBy(() -> society.switchOn(AddOn.FEED))
                .isInstanceOf(InvalidResidencyException.class).hasMessageContaining("not available");

        society.makeAvailable(AddOn.FEED);
        assertThat(society.isAvailable(AddOn.FEED)).isTrue();
        assertThat(society.isOn(AddOn.FEED)).isFalse();

        society.switchOn(AddOn.FEED);
        assertThat(society.isOn(AddOn.FEED)).isTrue();
        society.switchOff(AddOn.FEED);
        assertThat(society.isOn(AddOn.FEED)).isFalse();
    }

    @Test
    void withdrawingAnAddOnAlsoSwitchesItOff() {
        Society society = new Society(new SocietyId(1), "Runwal MyCity", RUNWAL,
                Set.of(AddOn.FEED, AddOn.COMPLAINTS), Set.of(AddOn.FEED, AddOn.COMPLAINTS));

        society.withdraw(AddOn.COMPLAINTS);

        assertThat(society.isOn(AddOn.COMPLAINTS)).isFalse();
        assertThat(society.isAvailable(AddOn.COMPLAINTS)).isFalse();
        assertThat(society.enabledAddOns()).containsExactly(AddOn.FEED);
        assertThatThrownBy(() -> new Society(new SocietyId(2), "X", null, Set.of(), Set.of(AddOn.FEED)))
                .isInstanceOf(InvalidResidencyException.class);
    }

    @Test
    void neighboursAreWithinTheDistanceAndNeedAKnownLocation() {
        Society runwal = new Society(new SocietyId(1), "Runwal MyCity", RUNWAL, Set.of(), Set.of());
        Society palava = new Society(new SocietyId(2), "Palava", PALAVA, Set.of(), Set.of());
        Society thane = new Society(new SocietyId(3), "Thane", THANE, Set.of(), Set.of());
        Society unknown = new Society(new SocietyId(4), "Somewhere", null, Set.of(), Set.of());

        assertThat(runwal.isNear(runwal, 10_000)).isTrue();
        assertThat(runwal.isNear(palava, 10_000)).isTrue();
        assertThat(runwal.isNear(thane, 10_000)).isFalse();
        assertThat(runwal.isNear(unknown, 10_000)).isFalse();
        assertThat(unknown.isNear(unknown, 10_000)).isFalse();
    }

    @Test
    void addOnNamesAreChecked() {
        assertThat(AddOn.from("COMPLAINTS")).isEqualTo(AddOn.COMPLAINTS);
        assertThatThrownBy(() -> AddOn.from("CHAT")).isInstanceOf(InvalidResidencyException.class);
        assertThatThrownBy(() -> new GeoPoint(100, 0)).isInstanceOf(InvalidResidencyException.class);
    }

    @Test
    void aTowerWithItsOwnComplaintAppCanBeLeftOutAndJoinLater() {
        TowerId b2 = new TowerId(5);
        TowerId c1 = new TowerId(7);
        Society society = new Society(new SocietyId(1), "Runwal MyCity", RUNWAL,
                Set.of(AddOn.COMPLAINTS), Set.of(AddOn.COMPLAINTS));

        society.switchOffInTower(AddOn.COMPLAINTS, b2);
        assertThat(society.isOnInTower(AddOn.COMPLAINTS, b2)).isFalse();
        assertThat(society.isOnInTower(AddOn.COMPLAINTS, c1)).isTrue();
        assertThat(society.isOn(AddOn.COMPLAINTS)).as("still on for the society").isTrue();
        assertThat(society.towersWithoutComplaints()).containsExactly(b2);

        society.switchOnInTower(AddOn.COMPLAINTS, b2);
        assertThat(society.isOnInTower(AddOn.COMPLAINTS, b2)).isTrue();
        assertThat(society.towersWithoutComplaints()).isEmpty();
    }

    @Test
    void noTowerHasComplaintsWhileTheSocietyHasThemOff() {
        Society society = new Society(new SocietyId(1), "Runwal MyCity", RUNWAL,
                Set.of(AddOn.COMPLAINTS), Set.of(), Set.of());
        assertThat(society.isOnInTower(AddOn.COMPLAINTS, new TowerId(5))).isFalse();
    }

    @Test
    void onlyComplaintsCanBeSwitchedPerTower() {
        Society society = new Society(new SocietyId(1), "Runwal MyCity", RUNWAL, Set.of(AddOn.FEED), Set.of(AddOn.FEED));
        assertThatThrownBy(() -> society.switchOffInTower(AddOn.FEED, new TowerId(5)))
                .isInstanceOf(InvalidResidencyException.class).hasMessageContaining("Only complaints");
    }
}
