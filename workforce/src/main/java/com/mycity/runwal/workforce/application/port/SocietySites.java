package com.mycity.runwal.workforce.application.port;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.workforce.domain.model.shift.Site;

/** Where each society's grounds are, for checking where staff clock in and out. */
public interface SocietySites {

    /** The society's site; every society that has staff must have one configured. */
    Site siteOf(SocietyId societyId);
}
