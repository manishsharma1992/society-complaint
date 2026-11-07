package com.mycity.runwal.residency.domain.service;

import com.mycity.runwal.residency.domain.model.actor.Actor;
import com.mycity.runwal.shared.ddd.annotation.DomainService;
import com.mycity.runwal.shared.id.SocietyId;

/** Who decides a society's add-ons: the platform owner offers them, the society admin switches them on or off. */
@DomainService
public class SocietySettingsPolicy {

    public boolean mayOfferAddOns(Actor actor) {
        return actor.active() && actor.platformOwner();
    }

    public boolean maySwitchAddOns(Actor actor, SocietyId societyId) {
        return actor.active() && (actor.platformOwner() || actor.isSocietyAdminOf(societyId));
    }
}
