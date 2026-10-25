package com.mycity.runwal.residency.domain.model.flat;

import com.mycity.runwal.shared.ddd.annotation.DomainEntity;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

/**
 * A person linked to a flat as owner or tenant, identified by the user within the flat.
 * Only reachable through {@link Flat}; outside code sees {@link MembershipSnapshot}s.
 */
@DomainEntity
final class Membership {

    private final UserId userId;
    private final MembershipType type;
    private final Instant since;

    Membership(UserId userId, MembershipType type, Instant since) {
        this.userId = userId;
        this.type = type;
        this.since = since;
    }

    static Membership from(MembershipSnapshot snapshot) {
        return new Membership(snapshot.userId(), snapshot.type(), snapshot.since());
    }

    UserId userId() {
        return userId;
    }

    MembershipSnapshot snapshot() {
        return new MembershipSnapshot(userId, type, since);
    }
}
