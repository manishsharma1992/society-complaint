package com.mycity.runwal.integration;

import com.mycity.runwal.complaint.application.port.StaffOnDuty;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.application.query.StaffOnDutyQuery;
import com.mycity.runwal.workforce.application.service.ShiftQueryService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Answers Complaint Management's "who is on shift?" from Workforce, where shifts are kept. */
@Component
class InProcessStaffOnDuty implements StaffOnDuty {

    private final ShiftQueryService shifts;

    InProcessStaffOnDuty(ShiftQueryService shifts) {
        this.shifts = shifts;
    }

    @Override
    public Set<UserId> onDuty(SocietyId societyId) {
        return shifts.staffOnDuty(new StaffOnDutyQuery(societyId.value())).stream().map(UserId::new)
                .collect(Collectors.toSet());
    }
}
