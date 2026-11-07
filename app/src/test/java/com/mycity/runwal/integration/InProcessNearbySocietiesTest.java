package com.mycity.runwal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mycity.runwal.feed.application.query.NeighbourView;
import com.mycity.runwal.residency.application.query.NearbySocietiesQuery;
import com.mycity.runwal.residency.application.query.SocietyView;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;
import org.junit.jupiter.api.Test;

class InProcessNearbySocietiesTest {

    @Test
    void asksResidencyForFeedSocietiesWithinTheConfiguredDistance() {
        ResidencyQueryService residency = mock(ResidencyQueryService.class);
        given(residency.nearby(new NearbySocietiesQuery(1, 10_000, "FEED"))).willReturn(List.of(
                new SocietyView(1, "Runwal MyCity", 19.186062, 73.074812, List.of()),
                new SocietyView(2, "Palava", 19.158, 73.03, List.of())));

        assertThat(new InProcessNearbySocieties(residency, 10_000).of(new SocietyId(1))).containsExactly(
                new NeighbourView(new SocietyId(1), "Runwal MyCity"), new NeighbourView(new SocietyId(2), "Palava"));
    }
}
