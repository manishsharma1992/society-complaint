package com.mycity.runwal.complaint.domain.model.assignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.complaint.domain.event.AutoAssignmentSwitched;
import com.mycity.runwal.complaint.domain.event.StaffSkillsChanged;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AssignmentSettingsTest {

    private static final SocietyId SOCIETY = new SocietyId(1);
    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");

    @Test
    void automaticAssignmentStartsOffAndSaysWhenItChanges() {
        AutoAssignment auto = AutoAssignment.offFor(SOCIETY);
        assertThat(auto.isOn()).isFalse();

        auto.switchOn(NOW);
        auto.switchOn(NOW);
        assertThat(auto.isOn()).isTrue();
        assertThat(auto.pullDomainEvents()).as("once, not for a repeat").containsExactly(new AutoAssignmentSwitched(SOCIETY, true, NOW));

        auto.switchOff(NOW);
        assertThat(auto.pullDomainEvents()).containsExactly(new AutoAssignmentSwitched(SOCIETY, false, NOW));
    }

    @Test
    void theAdminSetsWhichCategoriesAStaffMemberHandles() {
        StaffSkills raju = StaffSkills.none(SOCIETY, new UserId(30));
        assertThat(raju.handles(new ComplaintCategoryId(1))).isFalse();

        raju.handleOnly(Set.of(new ComplaintCategoryId(1), new ComplaintCategoryId(3)), NOW);
        assertThat(raju.handles(new ComplaintCategoryId(1))).isTrue();
        assertThat(raju.handles(new ComplaintCategoryId(2))).isFalse();
        assertThat(raju.pullDomainEvents()).containsExactly(new StaffSkillsChanged(SOCIETY, new UserId(30), NOW));
    }
}
