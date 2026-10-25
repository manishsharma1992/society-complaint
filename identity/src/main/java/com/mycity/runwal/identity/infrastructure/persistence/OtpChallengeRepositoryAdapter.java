package com.mycity.runwal.identity.infrastructure.persistence;

import com.mycity.runwal.identity.domain.model.otp.OtpChallenge;
import com.mycity.runwal.identity.domain.model.otp.OtpChallengeRepository;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class OtpChallengeRepositoryAdapter implements OtpChallengeRepository {

    private final OtpChallengeJpaRepository jpaRepository;
    private final EntityManager entityManager;

    OtpChallengeRepositoryAdapter(OtpChallengeJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(OtpChallenge challenge) {
        jpaRepository.findById(challenge.phone().value()).ifPresentOrElse(
                existing -> IdentityMapper.copy(challenge, existing),
                () -> {
                    OtpChallengeJpaEntity entity = new OtpChallengeJpaEntity();
                    IdentityMapper.copy(challenge, entity);
                    entityManager.persist(entity);
                });
    }

    @Override
    public Optional<OtpChallenge> findByPhone(PhoneNumber phone) {
        return jpaRepository.findById(phone.value()).map(IdentityMapper::toDomain);
    }
}
