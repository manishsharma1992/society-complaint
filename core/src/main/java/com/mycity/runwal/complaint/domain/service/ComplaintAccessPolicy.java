package com.mycity.runwal.complaint.domain.service;

import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintScope;
import com.mycity.runwal.shared.ddd.annotation.DomainService;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.Set;

/**
 * Who may do what with complaints. Managers are the society admin and the tower admin of the complaint's tower.
 * Every rule needs an active account.
 */
@DomainService
public class ComplaintAccessPolicy {

    /** Residents raise complaints for their own flats; anyone in the society may report a common area. */
    public boolean mayRaise(Actor actor, SocietyId societyId, ComplaintLocation location, Set<FlatId> actorFlats) {
        if (!actor.active() || !actor.belongsTo(societyId)) {
            return false;
        }
        return location.scope() == ComplaintScope.COMMON_AREA || actor.worksIn(societyId)
                || actorFlats.contains(location.flatId());
    }

    /** Admins and staff see everything; residents see their flats', their own and all common-area complaints. */
    public boolean maySee(Actor actor, Complaint complaint, Set<FlatId> actorFlats) {
        if (!actor.active()) {
            return false;
        }
        if (actor.worksIn(complaint.societyId())) {
            return true;
        }
        ComplaintLocation location = complaint.location();
        return actor.belongsTo(complaint.societyId()) && (actor.is(complaint.raisedBy())
                || location.scope() == ComplaintScope.COMMON_AREA || actorFlats.contains(location.flatId()));
    }

    /** Admins see the whole list; everyone else gets the filtered list (see {@link #maySee}). */
    public boolean maySeeAll(Actor actor, SocietyId societyId) {
        return actor.active() && actor.worksIn(societyId);
    }

    public boolean mayAssignOrReject(Actor actor, Complaint complaint) {
        return actor.active() && manages(actor, complaint);
    }

    /** Starting and resolving is done by the assigned staff member (or a manager). */
    public boolean mayWorkOn(Actor actor, Complaint complaint) {
        return actor.active()
                && (complaint.assignedTo().map(actor::is).orElse(false) || manages(actor, complaint));
    }

    /** Closing confirms the fix: the person who raised it, or a manager. */
    public boolean mayClose(Actor actor, Complaint complaint) {
        return actor.active() && (actor.is(complaint.raisedBy()) || manages(actor, complaint));
    }

    /** Whoever may see a complaint may talk about it (e.g. "same problem in my flat" on a common-area complaint). */
    public boolean mayComment(Actor actor, Complaint complaint, Set<FlatId> actorFlats) {
        return maySee(actor, complaint, actorFlats);
    }

    public boolean mayAddPhoto(Actor actor, Complaint complaint) {
        return actor.active() && (actor.is(complaint.raisedBy())
                || complaint.assignedTo().map(actor::is).orElse(false) || manages(actor, complaint));
    }

    private static boolean manages(Actor actor, Complaint complaint) {
        return actor.manages(complaint.societyId(), complaint.location().towerId());
    }
}
