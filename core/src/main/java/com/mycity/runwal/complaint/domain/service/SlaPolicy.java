package com.mycity.runwal.complaint.domain.service;

import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.shared.ddd.annotation.DomainService;
import java.time.Instant;

/**
 * Decides when a complaint is due. Today the deadline is simply the category's SLA after raising;
 * rules such as office hours or holidays will go here.
 */
@DomainService
public class SlaPolicy {

    public Instant dueAt(ComplaintCategory category, Instant raisedAt) {
        return raisedAt.plus(category.sla());
    }
}
