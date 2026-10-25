package com.mycity.runwal.identity.application.port;

import com.mycity.runwal.shared.id.UserId;

/** Who is making the current request (the subject of the verified login token). */
public interface CurrentUser {

    UserId userId();
}
