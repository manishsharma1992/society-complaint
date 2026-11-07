package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.shared.id.SocietyId;
import java.util.Objects;

/** A society whose published posts the feed shows (the viewer's own included), with the name to label them by. */
public record NeighbourView(SocietyId societyId, String name) {

    public NeighbourView {
        Objects.requireNonNull(societyId, "societyId");
        Objects.requireNonNull(name, "name");
    }
}
