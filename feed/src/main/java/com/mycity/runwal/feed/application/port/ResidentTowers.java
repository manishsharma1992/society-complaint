package com.mycity.runwal.feed.application.port;

import com.mycity.runwal.feed.domain.model.post.ShownTower;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Optional;

/** Where residents live (answered by the Residency context). */
public interface ResidentTowers {

    /** The tower as it is shown on a post, if the resident lives in it; empty otherwise. */
    Optional<ShownTower> towerOf(SocietyId societyId, UserId resident, TowerId towerId);
}
