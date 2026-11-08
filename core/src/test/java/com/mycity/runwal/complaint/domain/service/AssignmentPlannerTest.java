package com.mycity.runwal.complaint.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.complaint.domain.model.assignment.PlannedAssignment;
import com.mycity.runwal.complaint.domain.model.assignment.StaffMember;
import com.mycity.runwal.complaint.domain.model.assignment.StaffSkills;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintScope;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AssignmentPlannerTest {

    private static final SocietyId SOCIETY = new SocietyId(1);
    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");
    private static final ComplaintCategory PLUMBING =
            new ComplaintCategory(new ComplaintCategoryId(1), SOCIETY, "Plumbing", Duration.ofHours(24), true);
    private static final ComplaintCategory LIFT =
            new ComplaintCategory(new ComplaintCategoryId(3), SOCIETY, "Lift", Duration.ofHours(4), true);
    private static final TowerId B2 = new TowerId(5);
    private static final TowerId C1 = new TowerId(7);
    private static final UserId RAJU = new UserId(30);
    private static final UserId SUNIL = new UserId(31);

    private final AssignmentPlanner planner = new AssignmentPlanner();

    private static Complaint complaint(long id, ComplaintCategory category, TowerId tower, Duration raisedAgo) {
        ComplaintLocation where = tower == null
                ? new ComplaintLocation(ComplaintScope.COMMON_AREA, null, null)
                : ComplaintLocation.flat(tower, new FlatId(id));
        return Complaint.raise(new ComplaintId(id), SOCIETY, where, category, new ComplaintTitle("Problem " + id), null,
                new UserId(10), NOW.minus(raisedAgo), new SlaPolicy());
    }

    private static StaffSkills skills(UserId staff, ComplaintCategory... categories) {
        return new StaffSkills(SOCIETY, staff, java.util.Arrays.stream(categories).map(ComplaintCategory::id)
                .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void theMostUrgentComplaintGoesToAFreeStaffMemberWhoHandlesItsCategory() {
        Complaint leak = complaint(1, PLUMBING, B2, Duration.ofHours(2));   // due in 22 h
        Complaint lift = complaint(2, LIFT, B2, Duration.ofHours(1));       // due in 3 h: more urgent
        List<PlannedAssignment> plan = planner.plan(List.of(leak, lift),
                List.of(new StaffMember(RAJU, Set.of()), new StaffMember(SUNIL, Set.of())),
                List.of(skills(RAJU, PLUMBING), skills(SUNIL, PLUMBING, LIFT)));

        assertThat(plan).containsExactly(
                new PlannedAssignment(lift.id(), SUNIL),
                new PlannedAssignment(leak.id(), RAJU));
    }

    @Test
    void eachStaffMemberGetsOneJobAndTheRestWait() {
        List<PlannedAssignment> plan = planner.plan(
                List.of(complaint(1, PLUMBING, B2, Duration.ofHours(3)), complaint(2, PLUMBING, B2, Duration.ofHours(1))),
                List.of(new StaffMember(RAJU, Set.of())), List.of(skills(RAJU, PLUMBING)));

        assertThat(plan).containsExactly(new PlannedAssignment(new ComplaintId(1), RAJU));
    }

    @Test
    void nobodyGetsACategoryTheyDoNotHandleOrATowerTheyDoNotWorkIn() {
        Complaint liftInC1 = complaint(1, LIFT, C1, Duration.ofHours(1));
        Complaint leakInC1 = complaint(2, PLUMBING, C1, Duration.ofHours(1));
        List<PlannedAssignment> plan = planner.plan(List.of(liftInC1, leakInC1),
                List.of(new StaffMember(RAJU, Set.of(B2)), new StaffMember(SUNIL, Set.of())),
                List.of(skills(RAJU, PLUMBING, LIFT), skills(SUNIL, PLUMBING)));

        assertThat(plan).as("Raju only works in B2; Sunil does not do lifts")
                .containsExactly(new PlannedAssignment(leakInC1.id(), SUNIL));
    }

    @Test
    void towerStaffAlsoTakeSocietyWideAreasAndStaffWithoutSkillsGetNothing() {
        Complaint clubhouse = complaint(1, PLUMBING, null, Duration.ofHours(1));
        assertThat(planner.plan(List.of(clubhouse), List.of(new StaffMember(RAJU, Set.of(B2))), List.of(skills(RAJU, PLUMBING))))
                .containsExactly(new PlannedAssignment(clubhouse.id(), RAJU));
        assertThat(planner.plan(List.of(clubhouse), List.of(new StaffMember(SUNIL, Set.of())), List.of())).isEmpty();
    }
}
