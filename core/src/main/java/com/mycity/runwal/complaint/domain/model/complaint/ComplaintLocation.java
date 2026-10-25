package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.TowerId;

/**
 * Where the problem is. A FLAT complaint names a flat and its tower. A COMMON_AREA complaint names no flat;
 * its tower is optional (a lift in T3 has one, the society garden has none).
 */
@ValueObject
public record ComplaintLocation(ComplaintScope scope, TowerId towerId, FlatId flatId) {

    public ComplaintLocation {
        if (scope == null) {
            throw new InvalidComplaintException("A complaint location needs a scope");
        }
        if (scope == ComplaintScope.FLAT && (flatId == null || towerId == null)) {
            throw new InvalidComplaintException("A flat complaint needs a flat and its tower");
        }
        if (scope == ComplaintScope.COMMON_AREA && flatId != null) {
            throw new InvalidComplaintException("A common-area complaint must not name a flat");
        }
    }

    public static ComplaintLocation flat(TowerId towerId, FlatId flatId) {
        return new ComplaintLocation(ComplaintScope.FLAT, towerId, flatId);
    }

    /** @param towerId the tower the common area belongs to, or {@code null} for society-wide areas */
    public static ComplaintLocation commonArea(TowerId towerId) {
        return new ComplaintLocation(ComplaintScope.COMMON_AREA, towerId, null);
    }
}
