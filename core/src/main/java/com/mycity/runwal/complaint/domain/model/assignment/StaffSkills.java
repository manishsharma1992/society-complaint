package com.mycity.runwal.complaint.domain.model.assignment;

import com.mycity.runwal.complaint.domain.event.StaffSkillsChanged;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.shared.ddd.AbstractAggregateRoot;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The complaint categories a staff member handles (Plumbing, Electrical, Lift, ...). Automatic assignment gives a
 * staff member only complaints of these categories; a staff member with none gets nothing automatically.
 */
@AggregateRoot
public class StaffSkills extends AbstractAggregateRoot {

    private final SocietyId societyId;
    private final UserId staffId;
    private final Set<ComplaintCategoryId> categories;

    public StaffSkills(SocietyId societyId, UserId staffId, Set<ComplaintCategoryId> categories) {
        this.societyId = Objects.requireNonNull(societyId, "societyId");
        this.staffId = Objects.requireNonNull(staffId, "staffId");
        this.categories = new HashSet<>(Objects.requireNonNull(categories, "categories"));
    }

    public static StaffSkills none(SocietyId societyId, UserId staffId) {
        return new StaffSkills(societyId, staffId, Set.of());
    }

    /** The society admin sets what this staff member handles. */
    public void handleOnly(Set<ComplaintCategoryId> newCategories, Instant at) {
        categories.clear();
        categories.addAll(Objects.requireNonNull(newCategories, "newCategories"));
        recordEvent(new StaffSkillsChanged(societyId, staffId, at));
    }

    public boolean handles(ComplaintCategoryId category) {
        return categories.contains(category);
    }

    public SocietyId societyId() {
        return societyId;
    }

    public UserId staffId() {
        return staffId;
    }

    public Set<ComplaintCategoryId> categories() {
        return Set.copyOf(categories);
    }
}
