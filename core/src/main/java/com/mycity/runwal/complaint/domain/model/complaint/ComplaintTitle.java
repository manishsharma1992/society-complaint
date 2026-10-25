package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;

/** Short summary of the problem, e.g. "Water leaking from kitchen ceiling". */
@ValueObject
public record ComplaintTitle(String value) {

    public static final int MAX_LENGTH = 150;

    public ComplaintTitle {
        if (value == null || value.isBlank()) {
            throw new InvalidComplaintException("A complaint needs a title");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidComplaintException("A complaint title can have at most " + MAX_LENGTH + " characters");
        }
    }
}
