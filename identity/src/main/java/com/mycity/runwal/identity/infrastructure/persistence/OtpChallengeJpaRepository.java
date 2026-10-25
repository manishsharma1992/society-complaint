package com.mycity.runwal.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface OtpChallengeJpaRepository extends JpaRepository<OtpChallengeJpaEntity, String> {
}
