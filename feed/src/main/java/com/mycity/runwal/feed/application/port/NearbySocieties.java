package com.mycity.runwal.feed.application.port;

import com.mycity.runwal.feed.application.query.NeighbourView;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;

/** The societies near this one (itself included) that use the feed (decided in the Residency context). */
public interface NearbySocieties {

    List<NeighbourView> of(SocietyId societyId);
}
