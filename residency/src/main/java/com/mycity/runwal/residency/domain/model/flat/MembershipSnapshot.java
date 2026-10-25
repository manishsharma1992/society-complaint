package com.mycity.runwal.residency.domain.model.flat;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

/** Read-only copy of a membership, used to read and to rebuild a flat's members. */
@ValueObject
public record MembershipSnapshot(UserId userId, MembershipType type, Instant since) {
}
