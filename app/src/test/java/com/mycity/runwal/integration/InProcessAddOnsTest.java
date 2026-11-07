package com.mycity.runwal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mycity.runwal.residency.application.query.AddOnOnQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.SocietyId;
import org.junit.jupiter.api.Test;

/** Complaints and staff shifts follow the COMPLAINTS add-on, the feed follows FEED, each as Residency says. */
class InProcessAddOnsTest {

    private final ResidencyQueryService residency = mock(ResidencyQueryService.class);

    @Test
    void eachContextAsksResidencyForItsOwnAddOn() {
        given(residency.addOnOn(new AddOnOnQuery(1, "COMPLAINTS"))).willReturn(true);
        given(residency.addOnOn(new AddOnOnQuery(1, "FEED"))).willReturn(false);

        assertThat(new InProcessComplaintsAddOn(residency).isOnFor(new SocietyId(1))).isTrue();
        assertThat(new InProcessWorkforceAddOn(residency).isOnFor(new SocietyId(1))).isTrue();
        assertThat(new InProcessFeedAddOn(residency).isOnFor(new SocietyId(1))).isFalse();
    }
}
