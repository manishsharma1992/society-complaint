package com.mycity.runwal.identity.application.port;

import com.mycity.runwal.identity.domain.model.user.PhoneNumber;

/** Makes the next one-time code for a phone: random, except the fixed code of configured demo numbers. */
public interface OtpCodeSource {

    String newCode(PhoneNumber phone);
}
