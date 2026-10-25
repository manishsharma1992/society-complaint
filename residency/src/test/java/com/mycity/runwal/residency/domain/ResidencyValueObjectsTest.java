package com.mycity.runwal.residency.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.residency.domain.model.flat.FlatNumber;
import com.mycity.runwal.residency.domain.model.flat.MembershipType;
import com.mycity.runwal.residency.domain.model.society.Society;
import com.mycity.runwal.residency.domain.model.tower.Tower;
import com.mycity.runwal.residency.domain.model.tower.TowerCode;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class ResidencyValueObjectsTest {

    @ParameterizedTest
    @CsvSource({"5, 3, 503, B2-503", "1, 1, 101, A1-101", "27, 4, 2704, A4-2704", "16, 2, 1602, C4-1602"})
    void flatNumberAsResidentsSayIt(int floor, int unit, int value, String label) {
        FlatNumber number = new FlatNumber(floor, unit);
        TowerCode tower = new TowerCode(label.substring(0, label.indexOf('-')));

        assertThat(number.value()).isEqualTo(value);
        assertThat(number.label(tower)).isEqualTo(label);
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 0", "100, 1", "1, 100"})
    void flatNumberOutOfRange(int floor, int unit) {
        assertThatThrownBy(() -> new FlatNumber(floor, unit)).isInstanceOf(InvalidResidencyException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"B-2", "Tower 1", "ABCDEFGHIJK"})
    void towerCodeIsShortAndAlphanumeric(String code) {
        assertThatThrownBy(() -> new TowerCode(code)).isInstanceOf(InvalidResidencyException.class);
    }

    @Test
    void towerCodesOfThisAndOtherComplexes() {
        assertThat(new TowerCode("B2").value()).isEqualTo("B2");
        assertThat(new TowerCode("1").value()).isEqualTo("1");
    }

    @Test
    void towerAndSocietyNeedNamesAndFloors() {
        assertThat(new Tower(new TowerId(8), new SocietyId(1), new TowerCode("C4"), " Vega ", 16).name())
                .isEqualTo("Vega");
        assertThatThrownBy(() -> new Tower(new TowerId(8), new SocietyId(1), new TowerCode("C4"), " ", 16))
                .isInstanceOf(InvalidResidencyException.class);
        assertThatThrownBy(() -> new Tower(new TowerId(8), new SocietyId(1), new TowerCode("C4"), "Vega", 0))
                .isInstanceOf(InvalidResidencyException.class);
        assertThatThrownBy(() -> new Society(new SocietyId(1), null)).isInstanceOf(InvalidResidencyException.class);
    }

    @Test
    void membershipTypes() {
        assertThat(MembershipType.from("TENANT")).isEqualTo(MembershipType.TENANT);
        assertThatThrownBy(() -> MembershipType.from("FAMILY"))
                .isInstanceOf(InvalidResidencyException.class)
                .hasMessageContaining("[OWNER, TENANT]");
    }
}
