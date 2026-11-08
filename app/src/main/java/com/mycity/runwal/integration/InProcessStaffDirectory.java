package com.mycity.runwal.integration;

import com.mycity.runwal.complaint.application.port.StaffDirectory;
import com.mycity.runwal.complaint.domain.model.assignment.StaffMember;
import com.mycity.runwal.identity.application.query.StaffOfSocietyQuery;
import com.mycity.runwal.identity.application.service.UserDirectoryService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Answers Complaint Management's "which staff, working where?" from Identity, where roles are kept. */
@Component
class InProcessStaffDirectory implements StaffDirectory {

    private final UserDirectoryService users;

    InProcessStaffDirectory(UserDirectoryService users) {
        this.users = users;
    }

    @Override
    public List<StaffMember> staffOf(SocietyId societyId) {
        return users.staffOf(new StaffOfSocietyQuery(societyId.value())).stream()
                .map(staff -> new StaffMember(new UserId(staff.userId()),
                        staff.towerIds().stream().map(TowerId::new).collect(Collectors.toSet())))
                .toList();
    }
}
