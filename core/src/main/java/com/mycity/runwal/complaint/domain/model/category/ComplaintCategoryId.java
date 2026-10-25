package com.mycity.runwal.complaint.domain.model.category;

import com.mycity.runwal.complaint.domain.exception.InvalidComplaintCategoryException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;

@ValueObject
public record ComplaintCategoryId(long value) {

    public ComplaintCategoryId {
        if (value <= 0) {
            throw new InvalidComplaintCategoryException("ComplaintCategoryId must be positive, but was " + value);
        }
    }
}
