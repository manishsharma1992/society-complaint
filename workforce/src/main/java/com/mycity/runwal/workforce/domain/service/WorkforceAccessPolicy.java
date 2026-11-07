package com.mycity.runwal.workforce.domain.service;

import com.mycity.runwal.shared.ddd.annotation.DomainService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.workforce.domain.model.actor.Actor;
import com.mycity.runwal.workforce.domain.model.actor.ActorRole.Part;

/**
 * Who may do what with shifts. Staff clock themselves in and out and see their own shifts; the society admin and
 * tower admins see everyone's shifts (staff serve the whole society, not one tower).
 */
@DomainService
public class WorkforceAccessPolicy {

    public boolean mayWorkShifts(Actor actor, SocietyId societyId) {
        return actor.active() && actor.has(Part.STAFF, societyId);
    }

    public boolean maySeeAllShifts(Actor actor, SocietyId societyId) {
        return actor.active() && (actor.has(Part.SOCIETY_ADMIN, societyId) || actor.has(Part.TOWER_ADMIN, societyId));
    }
}
