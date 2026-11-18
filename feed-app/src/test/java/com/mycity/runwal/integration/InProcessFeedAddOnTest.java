package com.mycity.runwal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mycity.runwal.residency.application.query.AddOnOnQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.SocietyId;
import org.junit.jupiter.api.Test;

/** The feed follows the FEED add-on, as Residency says. */
class InProcessFeedAddOnTest {

    private final ResidencyQueryService residency = mock(ResidencyQueryService.class);

    @Test
    void theFeedAsksResidencyForItsOwnAddOn() {
        given(residency.addOnOn(new AddOnOnQuery(1, "FEED"))).willReturn(false);
        given(residency.addOnOn(new AddOnOnQuery(2, "FEED"))).willReturn(true);

        assertThat(new InProcessFeedAddOn(residency).isOnFor(new SocietyId(1))).isFalse();
        assertThat(new InProcessFeedAddOn(residency).isOnFor(new SocietyId(2))).isTrue();
    }
}
