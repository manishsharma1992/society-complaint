package com.mycity.runwal.complaint.application.port;

import com.mycity.runwal.shared.id.SocietyId;

/** Whether the society has the Complaints add-on switched on (decided in the Residency context). */
public interface ComplaintsAddOn {

    boolean isOnFor(SocietyId societyId);
}
