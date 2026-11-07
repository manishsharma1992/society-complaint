package com.mycity.runwal.identity.infrastructure.persistence;

import com.mycity.runwal.identity.domain.model.otp.OtpChallenge;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.FlatClaim;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.identity.domain.model.user.RoleAssignment;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;

/** Maps the Identity aggregates to and from their JPA entities. */
final class IdentityMapper {

    private IdentityMapper() {
    }

    static AppUser toDomain(AppUserJpaEntity entity) {
        FlatClaim claim = entity.claimSocietyId == null ? null : new FlatClaim(new SocietyId(entity.claimSocietyId),
                new FlatId(entity.claimFlatId), entity.claimOccupancy);
        return new AppUser(new UserId(entity.id), new PhoneNumber(entity.phone), entity.createdAt, entity.name,
                entity.status, claim, entity.rejectionReason,
                entity.roles.stream().map(IdentityMapper::toRole).toList(), entity.platformOwner);
    }

    static AppUserJpaEntity toNewEntity(AppUser user) {
        AppUserJpaEntity entity = new AppUserJpaEntity();
        entity.id = user.id().value();
        entity.phone = user.phone().value();
        entity.createdAt = user.createdAt();
        updateEntity(user, entity);
        return entity;
    }

    /** Copies what changes over a user's life; role rows of roles the user keeps are left untouched. */
    static void updateEntity(AppUser user, AppUserJpaEntity entity) {
        entity.name = user.name().orElse(null);
        entity.status = user.status();
        entity.claimSocietyId = user.claim().map(c -> c.societyId().value()).orElse(null);
        entity.claimFlatId = user.claim().map(c -> c.flatId().value()).orElse(null);
        entity.claimOccupancy = user.claim().map(FlatClaim::occupancy).orElse(null);
        entity.rejectionReason = user.rejectionReason().orElse(null);
        List<RoleAssignment> roles = user.roles();
        entity.roles.removeIf(stored -> !roles.contains(toRole(stored)));
        List<RoleAssignment> stored = entity.roles.stream().map(IdentityMapper::toRole).toList();
        for (RoleAssignment role : roles) {
            if (!stored.contains(role)) {
                entity.roles.add(toEntity(role));
            }
        }
    }

    static OtpChallenge toDomain(OtpChallengeJpaEntity entity) {
        return new OtpChallenge(new PhoneNumber(entity.phone), entity.codeHash, entity.expiresAt,
                entity.failedAttempts, entity.used, entity.lastSentAt, entity.windowStart, entity.sendsInWindow);
    }

    static void copy(OtpChallenge challenge, OtpChallengeJpaEntity entity) {
        entity.phone = challenge.phone().value();
        entity.codeHash = challenge.codeHash();
        entity.expiresAt = challenge.expiresAt();
        entity.failedAttempts = challenge.failedAttempts();
        entity.used = challenge.isUsed();
        entity.lastSentAt = challenge.lastSentAt();
        entity.windowStart = challenge.windowStart();
        entity.sendsInWindow = challenge.sendsInWindow();
    }

    private static RoleAssignment toRole(UserRoleJpaEntity entity) {
        return new RoleAssignment(entity.role, new SocietyId(entity.societyId),
                entity.towerId == null ? null : new TowerId(entity.towerId));
    }

    private static UserRoleJpaEntity toEntity(RoleAssignment role) {
        UserRoleJpaEntity entity = new UserRoleJpaEntity();
        entity.role = role.role();
        entity.societyId = role.societyId().value();
        entity.towerId = role.towerId() == null ? null : role.towerId().value();
        return entity;
    }
}
