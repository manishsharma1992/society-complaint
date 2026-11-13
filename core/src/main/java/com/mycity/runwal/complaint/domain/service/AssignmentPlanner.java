package com.mycity.runwal.complaint.domain.service;

import com.mycity.runwal.complaint.domain.model.assignment.PlannedAssignment;
import com.mycity.runwal.complaint.domain.model.assignment.StaffMember;
import com.mycity.runwal.complaint.domain.model.assignment.StaffSkills;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import com.mycity.runwal.shared.ddd.annotation.DomainService;
import com.mycity.runwal.shared.id.UserId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Who gets which waiting complaint. The most urgent first (earliest due, then earliest raised); each goes to the
 * first free staff member who handles its category and works where it is. Each staff member gets one job at a time,
 * so a free staff member is one on shift with nothing assigned or in progress. Tower managers on call are always
 * free and stay available after a job; staff come before them.
 */
@DomainService
public class AssignmentPlanner {

    public List<PlannedAssignment> plan(List<Complaint> waiting, List<StaffMember> free, List<StaffSkills> skills) {
        Map<UserId, StaffSkills> skillsByStaff = skills.stream()
                .collect(Collectors.toMap(StaffSkills::staffId, Function.identity(), (a, b) -> a));
        Map<UserId, StaffMember> available = new LinkedHashMap<>();
        free.forEach(staff -> available.put(staff.id(), staff));

        List<PlannedAssignment> plan = new ArrayList<>();
        waiting.stream()
                .filter(c -> c.status() == ComplaintStatus.OPEN)
                .sorted(Comparator.comparing(Complaint::dueAt).thenComparing(Complaint::raisedAt))
                .forEach(complaint -> available.values().stream()
                        .filter(staff -> staff.covers(complaint.location().towerId()))
                        .filter(staff -> skillsByStaff.containsKey(staff.id())
                                && skillsByStaff.get(staff.id()).handles(complaint.categoryId()))
                        .findFirst()
                        .ifPresent(staff -> {
                            plan.add(new PlannedAssignment(complaint.id(), staff.id()));
                            if (!staff.onCall()) {
                                available.remove(staff.id());
                            }
                        }));
        return plan;
    }
}
