package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.TowerId;
import java.util.Objects;

/**
 * The author's tower, shown on the post only because the author chose to show it.
 *
 * @param label how the tower is shown, e.g. "B2 Radiant" (kept as it was when the post was submitted)
 */
@ValueObject
public record ShownTower(TowerId towerId, String label) {

    public ShownTower {
        Objects.requireNonNull(towerId, "towerId");
        if (label == null || label.isBlank() || label.length() > 120) {
            throw new InvalidPostException("A tower label has 1 to 120 characters");
        }
    }
}
