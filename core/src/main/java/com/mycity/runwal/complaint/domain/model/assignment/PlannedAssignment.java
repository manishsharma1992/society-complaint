package com.mycity.runwal.complaint.domain.model.assignment;

import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import com.mycity.runwal.shared.id.UserId;
import java.util.Objects;

/** One complaint to give to one staff member. */
@ValueObject
public record PlannedAssignment(ComplaintId complaintId, UserId staffId) {

    public PlannedAssignment {
        Objects.requireNonNull(complaintId, "complaintId");
        Objects.requireNonNull(staffId, "staffId");
    }
}
