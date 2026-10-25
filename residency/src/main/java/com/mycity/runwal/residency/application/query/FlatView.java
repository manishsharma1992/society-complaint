package com.mycity.runwal.residency.application.query;

import com.mycity.runwal.residency.domain.model.flat.Flat;
import com.mycity.runwal.residency.domain.model.tower.Tower;
import java.util.List;

/**
 * Read model of a flat, in plain types so the interfaces layer never sees the domain.
 *
 * @param number the flat number residents say, e.g. 503
 * @param label  the full label, e.g. "B2-503"
 */
public record FlatView(long id, long towerId, String towerCode, int floor, int number, String label,
                       List<FlatMemberView> members) {

    public static FlatView of(Flat flat, Tower tower) {
        return new FlatView(flat.id().value(), tower.id().value(), tower.code().value(), flat.number().floor(),
                flat.number().value(), flat.number().label(tower.code()),
                flat.members().stream().map(FlatMemberView::of).toList());
    }

    /** The flat without who lives there: what people other than the tower's admins see. */
    public FlatView withoutMembers() {
        return new FlatView(id, towerId, towerCode, floor, number, label, List.of());
    }
}
