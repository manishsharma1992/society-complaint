package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;

@ValueObject
public record ComplaintId(long value) {

    public ComplaintId {
        if (value <= 0) {
            throw new InvalidComplaintException("ComplaintId must be positive, but was " + value);
        }
    }
}
