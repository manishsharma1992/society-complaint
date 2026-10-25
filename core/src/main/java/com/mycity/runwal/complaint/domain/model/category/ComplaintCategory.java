package com.mycity.runwal.complaint.domain.model.category;

import com.mycity.runwal.complaint.domain.exception.InvalidComplaintCategoryException;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Duration;
import java.util.Objects;

/** A kind of complaint a society handles (e.g. "Plumbing"), with the time allowed to resolve it. */
@AggregateRoot
public class ComplaintCategory {

    private final ComplaintCategoryId id;
    private final SocietyId societyId;
    private final String name;
    private final Duration sla;
    private final boolean active;

    public ComplaintCategory(ComplaintCategoryId id, SocietyId societyId, String name, Duration sla, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.societyId = Objects.requireNonNull(societyId, "societyId");
        if (name == null || name.isBlank()) {
            throw new InvalidComplaintCategoryException("A complaint category needs a name");
        }
        if (sla == null || sla.isNegative() || sla.isZero()) {
            throw new InvalidComplaintCategoryException("The SLA of category '" + name + "' must be positive");
        }
        this.name = name.strip();
        this.sla = sla;
        this.active = active;
    }

    public boolean belongsTo(SocietyId society) {
        return societyId.equals(society);
    }

    public ComplaintCategoryId id() {
        return id;
    }

    public SocietyId societyId() {
        return societyId;
    }

    public String name() {
        return name;
    }

    public Duration sla() {
        return sla;
    }

    public boolean isActive() {
        return active;
    }
}
