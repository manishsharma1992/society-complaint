package com.mycity.runwal.complaint.application.port;

import com.mycity.runwal.complaint.domain.model.assignment.StaffMember;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;

/** The society's approved maintenance staff and the towers they work in, decided in the Identity context. */
public interface StaffDirectory {

    List<StaffMember> staffOf(SocietyId societyId);
}
