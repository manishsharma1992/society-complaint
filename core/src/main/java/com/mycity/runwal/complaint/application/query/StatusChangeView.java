package com.mycity.runwal.complaint.application.query;

import com.mycity.runwal.complaint.domain.model.complaint.StatusChangeSnapshot;
import java.time.Instant;

public record StatusChangeView(String fromStatus, String toStatus, long changedBy, Instant changedAt, String note) {

    static StatusChangeView of(StatusChangeSnapshot change) {
        return new StatusChangeView(
                change.from() == null ? null : change.from().name(),
                change.to().name(),
                change.changedBy().value(),
                change.changedAt(),
                change.note());
    }
}
