package com.mycity.runwal.identity.application.service;

import com.mycity.runwal.identity.domain.model.otp.OtpChallenge;
import com.mycity.runwal.identity.domain.model.otp.OtpChallengeRepository;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.AppUserRepository;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.identity.domain.model.user.Role;
import com.mycity.runwal.identity.domain.model.user.UserStatus;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory repositories for the Identity service tests. */
final class IdentityFakes {

    private IdentityFakes() {
    }

    static final class Users implements AppUserRepository {

        final Map<Long, AppUser> stored = new LinkedHashMap<>();
        private long sequence = 99;

        @Override
        public UserId nextId() {
            return new UserId(++sequence);
        }

        @Override
        public void save(AppUser user) {
            stored.put(user.id().value(), user);
        }

        @Override
        public Optional<AppUser> findById(UserId id) {
            return Optional.ofNullable(stored.get(id.value()));
        }

        @Override
        public Optional<AppUser> findByPhone(PhoneNumber phone) {
            return stored.values().stream().filter(u -> u.phone().equals(phone)).findFirst();
        }

        @Override
        public List<AppUser> findPendingRegistrations(SocietyId societyId) {
            return stored.values().stream()
                    .filter(u -> u.status() == UserStatus.PENDING
                            && u.claim().map(c -> c.societyId().equals(societyId)).orElse(false))
                    .toList();
        }

        @Override
        public List<AppUser> findAllById(Collection<UserId> ids) {
            return ids.stream().map(id -> stored.get(id.value())).filter(u -> u != null).toList();
        }

        @Override
        public List<AppUser> findWithRole(Role role, SocietyId societyId) {
            return stored.values().stream()
                    .filter(u -> u.status() == UserStatus.ACTIVE && u.hasRoleIn(role, societyId))
                    .sorted(Comparator.comparing(u -> u.name().orElse("")))
                    .toList();
        }
    }

    static final class Challenges implements OtpChallengeRepository {

        final Map<PhoneNumber, OtpChallenge> stored = new HashMap<>();

        @Override
        public void save(OtpChallenge challenge) {
            stored.put(challenge.phone(), challenge);
        }

        @Override
        public Optional<OtpChallenge> findByPhone(PhoneNumber phone) {
            return Optional.ofNullable(stored.get(phone));
        }
    }
}
