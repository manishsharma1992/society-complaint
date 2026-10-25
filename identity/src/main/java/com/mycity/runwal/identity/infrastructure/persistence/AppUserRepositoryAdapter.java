package com.mycity.runwal.identity.infrastructure.persistence;

import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.AppUserRepository;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.identity.domain.model.user.Role;
import com.mycity.runwal.identity.domain.model.user.UserStatus;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class AppUserRepositoryAdapter implements AppUserRepository {

    private final AppUserJpaRepository jpaRepository;
    private final EntityManager entityManager;

    AppUserRepositoryAdapter(AppUserJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public UserId nextId() {
        return new UserId(jpaRepository.nextId());
    }

    @Override
    public void save(AppUser user) {
        jpaRepository.findById(user.id().value()).ifPresentOrElse(
                existing -> IdentityMapper.updateEntity(user, existing),
                () -> entityManager.persist(IdentityMapper.toNewEntity(user)));
    }

    @Override
    public Optional<AppUser> findById(UserId id) {
        return jpaRepository.findById(id.value()).map(IdentityMapper::toDomain);
    }

    @Override
    public Optional<AppUser> findByPhone(PhoneNumber phone) {
        return jpaRepository.findByPhone(phone.value()).map(IdentityMapper::toDomain);
    }

    @Override
    public List<AppUser> findPendingRegistrations(SocietyId societyId) {
        return jpaRepository.findByStatusAndClaimSocietyIdOrderByCreatedAt(UserStatus.PENDING, societyId.value())
                .stream()
                .map(IdentityMapper::toDomain)
                .toList();
    }

    @Override
    public List<AppUser> findAllById(Collection<UserId> ids) {
        return jpaRepository.findAllById(ids.stream().map(UserId::value).toList()).stream()
                .map(IdentityMapper::toDomain)
                .toList();
    }

    @Override
    public List<AppUser> findWithRole(Role role, SocietyId societyId) {
        return jpaRepository.findWithRole(role, societyId.value(), UserStatus.ACTIVE).stream()
                .map(IdentityMapper::toDomain)
                .toList();
    }
}
