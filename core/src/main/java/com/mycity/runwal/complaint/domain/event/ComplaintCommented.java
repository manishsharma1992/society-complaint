package com.mycity.runwal.complaint.domain.event;

import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

public record ComplaintCommented(ComplaintId complaintId, UserId author, int sequence, Instant occurredOn)
        implements DomainEvent {
}
