package com.mycity.runwal.complaint.domain.event;

import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

public record ComplaintRejected(ComplaintId complaintId, UserId rejectedBy, String reason, Instant occurredOn) implements DomainEvent {
}
