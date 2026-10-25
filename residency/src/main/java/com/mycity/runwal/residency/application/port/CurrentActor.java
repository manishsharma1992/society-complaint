package com.mycity.runwal.residency.application.port;

import com.mycity.runwal.residency.domain.model.actor.Actor;

/** Who is making the current request (from the login token), in this context's terms. */
public interface CurrentActor {

    Actor actor();
}
