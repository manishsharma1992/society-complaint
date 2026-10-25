package com.mycity.runwal.residency.infrastructure.persistence;

import com.mycity.runwal.residency.domain.model.flat.Flat;
import com.mycity.runwal.residency.domain.model.flat.FlatNumber;
import com.mycity.runwal.residency.domain.model.flat.MembershipSnapshot;
import com.mycity.runwal.residency.domain.model.society.Society;
import com.mycity.runwal.residency.domain.model.tower.Tower;
import com.mycity.runwal.residency.domain.model.tower.TowerCode;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Maps the Residency aggregates to and from their JPA entities. */
final class ResidencyMapper {

    private ResidencyMapper() {
    }

    static Society toDomain(SocietyJpaEntity entity) {
        return new Society(new SocietyId(entity.id), entity.name);
    }

    static Tower toDomain(TowerJpaEntity entity) {
        return new Tower(new TowerId(entity.id), new SocietyId(entity.societyId), new TowerCode(entity.code),
                entity.name, entity.residentialFloors);
    }

    static Flat toDomain(FlatJpaEntity entity) {
        return new Flat(new FlatId(entity.id), new SocietyId(entity.societyId), new TowerId(entity.towerId),
                new FlatNumber(entity.floor, entity.unit),
                entity.members.stream().map(ResidencyMapper::toSnapshot).toList());
    }

    /**
     * Brings the stored members in line with the flat: removes unlinked people, adds newly linked ones.
     * Rows of people who stay linked are left untouched.
     */
    static void updateMembers(Flat flat, FlatJpaEntity entity) {
        List<MembershipSnapshot> members = flat.members();
        Set<Long> linked = members.stream().map(m -> m.userId().value()).collect(Collectors.toSet());
        entity.members.removeIf(stored -> !linked.contains(stored.userId));
        Set<Long> stored = entity.members.stream().map(m -> m.userId).collect(Collectors.toSet());
        for (MembershipSnapshot member : members) {
            if (!stored.contains(member.userId().value())) {
                entity.members.add(toEntity(member));
            }
        }
    }

    private static FlatMemberJpaEntity toEntity(MembershipSnapshot member) {
        FlatMemberJpaEntity entity = new FlatMemberJpaEntity();
        entity.userId = member.userId().value();
        entity.type = member.type();
        entity.since = member.since();
        return entity;
    }

    private static MembershipSnapshot toSnapshot(FlatMemberJpaEntity entity) {
        return new MembershipSnapshot(new UserId(entity.userId), entity.type, entity.since);
    }
}
