package com.mycity.runwal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mycity.runwal.complaint.domain.model.assignment.StaffMember;
import com.mycity.runwal.identity.application.query.StaffMemberView;
import com.mycity.runwal.identity.application.query.StaffOfSocietyQuery;
import com.mycity.runwal.identity.application.service.UserDirectoryService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.application.query.StaffOnDutyQuery;
import com.mycity.runwal.workforce.application.service.ShiftQueryService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Complaint Management learns who is on shift from Workforce and where staff work from Identity. */
class InProcessStaffTest {

    @Test
    void onShiftComesFromWorkforce() {
        ShiftQueryService shifts = mock(ShiftQueryService.class);
        given(shifts.staffOnDuty(new StaffOnDutyQuery(1))).willReturn(List.of(30L, 31L));

        assertThat(new InProcessStaffOnDuty(shifts).onDuty(new SocietyId(1))).containsExactlyInAnyOrder(new UserId(30), new UserId(31));
    }

    @Test
    void staffAndTheirTowersComeFromIdentity() {
        UserDirectoryService users = mock(UserDirectoryService.class);
        given(users.staffOf(new StaffOfSocietyQuery(1))).willReturn(List.of(
                new StaffMemberView(30, List.of()), new StaffMemberView(31, List.of(5L))));

        assertThat(new InProcessStaffDirectory(users).staffOf(new SocietyId(1))).containsExactly(
                new StaffMember(new UserId(30), Set.of()), new StaffMember(new UserId(31), Set.of(new TowerId(5))));
    }
}
