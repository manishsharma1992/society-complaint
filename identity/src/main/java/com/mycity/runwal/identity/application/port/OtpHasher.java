package com.mycity.runwal.identity.application.port;

import com.mycity.runwal.identity.domain.model.user.PhoneNumber;

/** One-way, keyed hash of a code for a phone, so stored codes are useless if the database leaks. */
public interface OtpHasher {

    String hash(PhoneNumber phone, String code);
}
