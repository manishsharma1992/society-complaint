package com.mycity.runwal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mycity.runwal.residency.application.query.AddOnOnInTowerQuery;
import com.mycity.runwal.residency.application.query.AddOnOnQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import org.junit.jupiter.api.Test;

/** Complaints and staff shifts follow the COMPLAINTS add-on, as Residency says. */
class InProcessAddOnsTest {

    private final ResidencyQueryService residency = mock(ResidencyQueryService.class);

    @Test
    void eachContextAsksResidencyForItsOwnAddOn() {
        given(residency.addOnOn(new AddOnOnQuery(1, "COMPLAINTS"))).willReturn(true);

        assertThat(new InProcessComplaintsAddOn(residency).isOnFor(new SocietyId(1))).isTrue();
        assertThat(new InProcessWorkforceAddOn(residency).isOnFor(new SocietyId(1))).isTrue();
    }

    @Test
    void complaintsAsksResidencyAboutTheTower() {
        given(residency.addOnOnInTower(new AddOnOnInTowerQuery(1, "COMPLAINTS", 5))).willReturn(false);
        given(residency.addOnOnInTower(new AddOnOnInTowerQuery(1, "COMPLAINTS", 6))).willReturn(true);

        assertThat(new InProcessComplaintsAddOn(residency).isOnIn(new SocietyId(1), new TowerId(5))).isFalse();
        assertThat(new InProcessComplaintsAddOn(residency).isOnIn(new SocietyId(1), new TowerId(6))).isTrue();
    }
}
