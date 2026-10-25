package com.mycity.runwal.complaint.domain.event;

import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;

public record ComplaintRaised(
        ComplaintId complaintId,
        SocietyId societyId,
        ComplaintCategoryId categoryId,
        UserId raisedBy,
        Instant dueAt,
        Instant occurredOn) implements DomainEvent {
}
