package com.mycity.runwal.feed.domain.service;

import com.mycity.runwal.feed.domain.model.actor.Actor;
import com.mycity.runwal.feed.domain.model.actor.ActorRole;
import com.mycity.runwal.shared.ddd.annotation.DomainService;
import com.mycity.runwal.shared.id.SocietyId;

/** Who may post to and moderate a society's public feed. */
@DomainService
public class FeedAccessPolicy {

    /** Only approved residents post: no anonymous posting, so posts stay accountable and spam stays out. */
    public boolean mayPost(Actor actor, SocietyId societyId) {
        return actor.active() && actor.has(ActorRole.Part.RESIDENT, societyId);
    }

    /** Moderators are the society admins (a scoped MODERATOR role only when the committee asks for it). */
    public boolean mayModerate(Actor actor, SocietyId societyId) {
        return actor.active() && actor.has(ActorRole.Part.SOCIETY_ADMIN, societyId);
    }
}
