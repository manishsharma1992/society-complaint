package com.mycity.runwal.complaint.application.port;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Set;

/** Who is on shift right now (clocked in and not yet out), decided in the Workforce context. */
public interface StaffOnDuty {

    Set<UserId> onDuty(SocietyId societyId);
}
