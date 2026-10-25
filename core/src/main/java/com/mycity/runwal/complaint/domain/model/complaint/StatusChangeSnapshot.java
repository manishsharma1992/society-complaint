package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

/**
 * Read-only copy of a status history entry, used to read and to rebuild a complaint's history.
 *
 * @param from {@code null} for the first entry (the complaint being raised)
 * @param note optional; the resolution note or rejection reason
 */
@ValueObject
public record StatusChangeSnapshot(
        int sequence, ComplaintStatus from, ComplaintStatus to, UserId changedBy, Instant changedAt, String note) {
}
