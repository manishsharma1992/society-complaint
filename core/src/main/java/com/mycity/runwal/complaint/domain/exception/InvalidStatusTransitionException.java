package com.mycity.runwal.complaint.domain.exception;

import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import com.mycity.runwal.shared.ddd.DomainException;

/** The complaint lifecycle does not allow moving from the current status to the requested one. */
public class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(ComplaintId complaintId, ComplaintStatus from, ComplaintStatus to) {
        super("Complaint " + complaintId.value() + " cannot move from " + from + " to " + to);
    }
}
