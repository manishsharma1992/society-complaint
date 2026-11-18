package com.mycity.runwal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mycity.runwal.feed.domain.model.post.ShownTower;
import com.mycity.runwal.residency.application.query.FlatView;
import com.mycity.runwal.residency.application.query.FlatsOfMemberQuery;
import com.mycity.runwal.residency.application.query.ListTowersQuery;
import com.mycity.runwal.residency.application.query.TowerView;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;
import org.junit.jupiter.api.Test;

class InProcessResidentTowersTest {

    private final ResidencyQueryService residency = mock(ResidencyQueryService.class);
    private final InProcessResidentTowers towers = new InProcessResidentTowers(residency);

    @Test
    void aResidentsOwnTowerIsShownByCodeAndName() {
        given(residency.flatsOfMember(new FlatsOfMemberQuery(1, 4)))
                .willReturn(List.of(new FlatView(795, 7, "B2", 5, 503, "B2-503", List.of())));
        given(residency.towers(new ListTowersQuery(1)))
                .willReturn(List.of(new TowerView(7, "B2", "Radiant", 27), new TowerView(8, "C4", "Vega", 27)));

        assertThat(towers.towerOf(new SocietyId(1), new UserId(4), new TowerId(7)))
                .contains(new ShownTower(new TowerId(7), "B2 Radiant"));
        assertThat(towers.towerOf(new SocietyId(1), new UserId(4), new TowerId(8))).isEmpty();
    }
}
