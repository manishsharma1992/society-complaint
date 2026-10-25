package com.mycity.runwal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mycity.runwal.residency.application.query.FlatView;
import com.mycity.runwal.residency.application.query.FlatsOfMemberQuery;
import com.mycity.runwal.residency.application.query.GetFlatQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InProcessFlatDirectoryTest {

    private final ResidencyQueryService residency = mock(ResidencyQueryService.class);
    private final InProcessFlatDirectory directory = new InProcessFlatDirectory(residency);

    @Test
    void towerComesFromResidency() {
        given(residency.flat(new GetFlatQuery(1, 795)))
                .willReturn(Optional.of(new FlatView(795, 7, "B2", 5, 503, "B2-503", List.of())));

        assertThat(directory.towerOf(new SocietyId(1), new FlatId(795))).contains(new TowerId(7));
    }

    @Test
    void flatsOfAUserComeFromResidency() {
        given(residency.flatsOfMember(new FlatsOfMemberQuery(1, 4)))
                .willReturn(List.of(new FlatView(795, 7, "B2", 5, 503, "B2-503", List.of())));

        assertThat(directory.flatsOf(new SocietyId(1), new UserId(4)))
                .containsExactly(new FlatId(795));
    }

    @Test
    void unknownFlatIsEmpty() {
        given(residency.flat(new GetFlatQuery(1, 99999))).willReturn(Optional.empty());

        assertThat(directory.towerOf(new SocietyId(1), new FlatId(99999))).isEmpty();
    }
}
