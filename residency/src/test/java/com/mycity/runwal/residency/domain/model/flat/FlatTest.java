package com.mycity.runwal.residency.domain.model.flat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.residency.domain.event.FlatMemberAdded;
import com.mycity.runwal.residency.domain.event.FlatMemberRemoved;
import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.residency.domain.exception.MembershipNotFoundException;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlatTest {

    private static final SocietyId SOCIETY = new SocietyId(1);
    private static final UserId OWNER = new UserId(10);
    private static final UserId TENANT = new UserId(11);
    private static final UserId ADMIN = new UserId(20);
    private static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");

    private static Flat flat(long id) {
        return new Flat(new FlatId(id), SOCIETY, new TowerId(7), new FlatNumber(5, 3), List.of());
    }

    @Test
    void linksOwnersAndTenantsInOrder() {
        Flat flat = flat(795);

        flat.addMember(OWNER, MembershipType.OWNER, ADMIN, NOW);
        flat.addMember(TENANT, MembershipType.TENANT, ADMIN, NOW.plusSeconds(60));

        assertThat(flat.members()).containsExactly(
                new MembershipSnapshot(OWNER, MembershipType.OWNER, NOW),
                new MembershipSnapshot(TENANT, MembershipType.TENANT, NOW.plusSeconds(60)));
        assertThat(flat.hasMember(TENANT)).isTrue();
        assertThat(flat.pullDomainEvents()).containsExactly(
                new FlatMemberAdded(flat.id(), SOCIETY, OWNER, MembershipType.OWNER, ADMIN, NOW),
                new FlatMemberAdded(flat.id(), SOCIETY, TENANT, MembershipType.TENANT, ADMIN, NOW.plusSeconds(60)));
    }

    @Test
    void samePersonOnlyOncePerFlat() {
        Flat flat = flat(795);
        flat.addMember(OWNER, MembershipType.OWNER, ADMIN, NOW);

        assertThatThrownBy(() -> flat.addMember(OWNER, MembershipType.TENANT, ADMIN, NOW))
                .isInstanceOf(InvalidResidencyException.class)
                .hasMessage("User 10 is already linked to flat 795");
        assertThat(flat.members()).hasSize(1);
    }

    @Test
    void onePersonCanBeLinkedToSeveralFlats() {
        Flat flat503 = flat(795);
        Flat flat504 = flat(796);

        flat503.addMember(OWNER, MembershipType.OWNER, ADMIN, NOW);
        flat504.addMember(OWNER, MembershipType.OWNER, ADMIN, NOW);

        assertThat(flat503.hasMember(OWNER)).isTrue();
        assertThat(flat504.hasMember(OWNER)).isTrue();
    }

    @Test
    void tenantMovesOut() {
        Flat flat = new Flat(new FlatId(795), SOCIETY, new TowerId(7), new FlatNumber(5, 3),
                List.of(new MembershipSnapshot(OWNER, MembershipType.OWNER, NOW),
                        new MembershipSnapshot(TENANT, MembershipType.TENANT, NOW)));

        flat.removeMember(TENANT, ADMIN, NOW.plusSeconds(3600));

        assertThat(flat.members()).extracting(MembershipSnapshot::userId).containsExactly(OWNER);
        assertThat(flat.pullDomainEvents())
                .containsExactly(new FlatMemberRemoved(flat.id(), SOCIETY, TENANT, ADMIN, NOW.plusSeconds(3600)));
    }

    @Test
    void removingSomeoneNotLinkedFails() {
        Flat flat = flat(795);

        assertThatThrownBy(() -> flat.removeMember(TENANT, ADMIN, NOW))
                .isInstanceOf(MembershipNotFoundException.class)
                .hasMessage("User 11 is not linked to flat 795");
        assertThat(flat.pullDomainEvents()).isEmpty();
    }
}
