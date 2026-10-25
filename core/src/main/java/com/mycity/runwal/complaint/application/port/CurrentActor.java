package com.mycity.runwal.complaint.application.port;

import com.mycity.runwal.complaint.domain.model.actor.Actor;

/** Who is making the current request (from the login token), in this context's terms. */
public interface CurrentActor {

    Actor actor();
}
