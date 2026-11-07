package com.mycity.runwal.integration;

import com.mycity.runwal.feed.application.port.NearbySocieties;
import com.mycity.runwal.feed.application.query.NeighbourView;
import com.mycity.runwal.residency.application.query.NearbySocietiesQuery;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Answers the feed's "which societies are near this one?" from Residency: those within {@code feed.nearby-meters}
 * (10 km by default) that have the Feed add-on on, the society itself included.
 */
@Component
class InProcessNearbySocieties implements NearbySocieties {

    private final ResidencyQueryService residency;
    private final double meters;

    InProcessNearbySocieties(ResidencyQueryService residency, @Value("${feed.nearby-meters:10000}") double meters) {
        this.residency = residency;
        this.meters = meters;
    }

    @Override
    public List<NeighbourView> of(SocietyId societyId) {
        return residency.nearby(new NearbySocietiesQuery(societyId.value(), meters, "FEED")).stream()
                .map(society -> new NeighbourView(new SocietyId(society.id()), society.name()))
                .toList();
    }
}
