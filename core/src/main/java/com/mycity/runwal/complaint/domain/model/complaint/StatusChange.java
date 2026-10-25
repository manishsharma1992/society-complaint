package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.shared.ddd.annotation.DomainEntity;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

/**
 * One entry in a complaint's status history, identified by its sequence number within the complaint.
 * Only reachable through {@link Complaint}; outside code sees {@link StatusChangeSnapshot}s.
 */
@DomainEntity
final class StatusChange {

    private final int sequence;
    private final ComplaintStatus from;
    private final ComplaintStatus to;
    private final UserId changedBy;
    private final Instant changedAt;
    private final String note;

    StatusChange(int sequence, ComplaintStatus from, ComplaintStatus to, UserId changedBy, Instant changedAt, String note) {
        this.sequence = sequence;
        this.from = from;
        this.to = to;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.note = note;
    }

    static StatusChange from(StatusChangeSnapshot snapshot) {
        return new StatusChange(snapshot.sequence(), snapshot.from(), snapshot.to(),
                snapshot.changedBy(), snapshot.changedAt(), snapshot.note());
    }

    StatusChangeSnapshot snapshot() {
        return new StatusChangeSnapshot(sequence, from, to, changedBy, changedAt, note);
    }
}
