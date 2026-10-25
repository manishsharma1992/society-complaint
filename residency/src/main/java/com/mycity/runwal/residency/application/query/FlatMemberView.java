package com.mycity.runwal.residency.application.query;

import com.mycity.runwal.residency.domain.model.flat.MembershipSnapshot;
import java.time.Instant;

public record FlatMemberView(long userId, String type, Instant since) {

    static FlatMemberView of(MembershipSnapshot member) {
        return new FlatMemberView(member.userId().value(), member.type().name(), member.since());
    }
}
