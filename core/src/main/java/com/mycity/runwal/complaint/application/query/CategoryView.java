package com.mycity.runwal.complaint.application.query;

import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;

/** A category to choose when raising a complaint, with the hours the society allows to resolve it. */
public record CategoryView(long id, String name, long slaHours) {

    public static CategoryView of(ComplaintCategory category) {
        return new CategoryView(category.id().value(), category.name(), category.sla().toHours());
    }
}
