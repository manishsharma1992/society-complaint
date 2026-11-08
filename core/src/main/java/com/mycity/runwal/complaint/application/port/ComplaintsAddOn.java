package com.mycity.runwal.complaint.application.port;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;

/** Whether the society uses the Complaints add-on (decided in the Residency context). */
public interface ComplaintsAddOn {

    /** Switched on for the society. */
    boolean isOnFor(SocietyId societyId);

    /** Switched on for the society and not switched off for this tower (a tower may handle complaints its own way). */
    boolean isOnIn(SocietyId societyId, TowerId towerId);
}
