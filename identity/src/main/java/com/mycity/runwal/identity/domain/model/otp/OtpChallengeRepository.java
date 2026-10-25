package com.mycity.runwal.identity.domain.model.otp;

import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import java.util.Optional;

@DomainRepository
public interface OtpChallengeRepository {

    void save(OtpChallenge challenge);

    Optional<OtpChallenge> findByPhone(PhoneNumber phone);
}
