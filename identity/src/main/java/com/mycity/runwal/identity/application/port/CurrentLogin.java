package com.mycity.runwal.identity.application.port;

import java.time.Instant;

/** The login behind the current request: when it ends and a new code (OTP) is needed. */
public interface CurrentLogin {

    Instant endsAt();
}
