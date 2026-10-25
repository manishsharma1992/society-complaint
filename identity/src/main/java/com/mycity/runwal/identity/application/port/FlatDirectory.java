package com.mycity.runwal.identity.application.port;

import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;

/** What Identity needs to know about flats, which the Residency context owns. */
public interface FlatDirectory {

    boolean exists(SocietyId societyId, FlatId flatId);
}
