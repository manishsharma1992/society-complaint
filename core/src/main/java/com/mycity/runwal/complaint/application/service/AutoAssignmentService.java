package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.command.SetStaffCategoriesCommand;
import com.mycity.runwal.complaint.application.command.SwitchAutoAssignmentCommand;
import com.mycity.runwal.complaint.application.port.ComplaintsAddOn;
import com.mycity.runwal.complaint.application.port.CurrentActor;
import com.mycity.runwal.complaint.application.port.DomainEventPublisher;
import com.mycity.runwal.complaint.application.port.StaffDirectory;
import com.mycity.runwal.complaint.application.port.StaffOnDuty;
import com.mycity.runwal.complaint.application.query.AutoAssignmentView;
import com.mycity.runwal.complaint.application.query.GetAutoAssignmentQuery;
import com.mycity.runwal.complaint.domain.exception.ComplaintCategoryNotFoundException;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.assignment.AutoAssignment;
import com.mycity.runwal.complaint.domain.model.assignment.AutoAssignmentRepository;
import com.mycity.runwal.complaint.domain.model.assignment.PlannedAssignment;
import com.mycity.runwal.complaint.domain.model.assignment.StaffMember;
import com.mycity.runwal.complaint.domain.model.assignment.StaffSkills;
import com.mycity.runwal.complaint.domain.model.assignment.StaffSkillsRepository;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryRepository;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintRepository;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import com.mycity.runwal.complaint.domain.service.AssignmentPlanner;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Automatic assignment: when a staff member frees up (resolves or rejects their job, starts a shift), a complaint
 * arrives, or the admin changes the setup, waiting complaints go to free staff on shift who handle their category,
 * or else to the tower manager when the admin gave them that category (e.g. lifts, serviced by an outside company).
 * The society admin switches it on and says who handles what; admins can still assign by hand.
 */
@ApplicationService
public class AutoAssignmentService {

    private static final int MAX_LOOKED_AT = 500;

    private final AutoAssignmentRepository settings;
    private final StaffSkillsRepository skills;
    private final ComplaintRepository complaints;
    private final ComplaintCategoryRepository categories;
    private final ComplaintsAddOn addOn;
    private final StaffOnDuty onDuty;
    private final StaffDirectory staffDirectory;
    private final AssignmentPlanner planner;
    private final ComplaintAccessPolicy policy;
    private final CurrentActor currentActor;
    private final DomainEventPublisher events;
    private final Clock clock;
    private final TransactionTemplate readOnly;
    private final TransactionTemplate writing;

    public AutoAssignmentService(AutoAssignmentRepository settings, StaffSkillsRepository skills,
                                 ComplaintRepository complaints, ComplaintCategoryRepository categories,
                                 ComplaintsAddOn addOn, StaffOnDuty onDuty, StaffDirectory staffDirectory,
                                 AssignmentPlanner planner, ComplaintAccessPolicy policy, CurrentActor currentActor,
                                 DomainEventPublisher events, Clock clock, PlatformTransactionManager transactions) {
        this.settings = settings;
        this.skills = skills;
        this.complaints = complaints;
        this.categories = categories;
        this.addOn = addOn;
        this.onDuty = onDuty;
        this.staffDirectory = staffDirectory;
        this.planner = planner;
        this.policy = policy;
        this.currentActor = currentActor;
        this.events = events;
        this.clock = clock;
        // runs after other transactions have committed (from event listeners), so each step starts its own
        this.readOnly = new TransactionTemplate(transactions);
        this.readOnly.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readOnly.setReadOnly(true);
        this.writing = new TransactionTemplate(transactions);
        this.writing.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Gives waiting complaints to free staff, the most urgent first; returns how many were assigned. Each assignment
     * is its own transaction (one aggregate each), checked again just before it is made.
     * shortcut: synchronized keeps two triggers from giving one staff member two jobs in this one JVM; use a
     * database lock when the contexts run as separate services.
     */
    public synchronized int assignWaiting(SocietyId societyId) {
        List<PlannedAssignment> plan = readOnly.execute(status -> plan(societyId));
        Set<UserId> onCall = staffDirectory.staffOf(societyId).stream().filter(StaffMember::onCall)
                .map(StaffMember::id).collect(Collectors.toSet());
        int assigned = 0;
        for (PlannedAssignment step : plan == null ? List.<PlannedAssignment>of() : plan) {
            Boolean done = writing.execute(status -> complaints.findById(societyId, step.complaintId())
                    .filter(complaint -> complaint.status() == ComplaintStatus.OPEN)
                    .filter(complaint -> onCall.contains(step.staffId())
                            || busyStaff(societyId).stream().noneMatch(step.staffId()::equals))
                    .map(complaint -> {
                        complaint.assignAutomatically(step.staffId(), clock.instant());
                        complaints.save(complaint);
                        events.publish(complaint.pullDomainEvents());
                        return true;
                    })
                    .orElse(false));
            assigned += Boolean.TRUE.equals(done) ? 1 : 0;
        }
        return assigned;
    }

    private List<PlannedAssignment> plan(SocietyId societyId) {
        if (!addOn.isOnFor(societyId) || !settings.find(societyId).isOn()) {
            return List.of();
        }
        List<Complaint> waiting = complaints.findBySociety(societyId, ComplaintStatus.OPEN, null, null, 0, MAX_LOOKED_AT);
        if (waiting.isEmpty()) {
            return List.of();
        }
        Set<UserId> working = onDuty.onDuty(societyId);
        Set<UserId> busy = busyStaff(societyId);
        List<StaffMember> free = staffDirectory.staffOf(societyId).stream()
                .filter(staff -> staff.onCall() || working.contains(staff.id()) && !busy.contains(staff.id()))
                .toList();
        return free.isEmpty() ? List.of() : planner.plan(waiting, free, skills.findBySociety(societyId));
    }

    /** Staff with a job assigned or in progress: one job at a time. */
    private Set<UserId> busyStaff(SocietyId societyId) {
        return Stream.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS)
                .flatMap(s -> complaints.findBySociety(societyId, s, null, null, 0, MAX_LOOKED_AT).stream())
                .flatMap(c -> c.assignedTo().stream())
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Transactional(readOnly = true)
    public AutoAssignmentView view(GetAutoAssignmentQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        Actor actor = currentActor.actor();
        if (!actor.active() || !actor.belongsTo(societyId)) {
            throw new ActionNotAllowedException("Only members of this society can see how complaints are assigned");
        }
        boolean on = settings.find(societyId).isOn();
        if (!policy.maySeeAssignment(actor, societyId)) {
            return new AutoAssignmentView(on, List.of());
        }
        return new AutoAssignmentView(on, skills.findBySociety(societyId).stream()
                .map(s -> new AutoAssignmentView.StaffCategoriesView(s.staffId().value(),
                        s.categories().stream().map(ComplaintCategoryId::value).sorted().toList()))
                .toList());
    }

    @Transactional
    public AutoAssignmentView switchOn(SwitchAutoAssignmentCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        if (!policy.mayManageAssignment(currentActor.actor(), societyId)) {
            throw new ActionNotAllowedException("Only the society admin can switch automatic assignment");
        }
        AutoAssignment auto = settings.find(societyId);
        if (command.on()) {
            auto.switchOn(clock.instant());
        } else {
            auto.switchOff(clock.instant());
        }
        settings.save(auto);
        events.publish(auto.pullDomainEvents());
        return view(new GetAutoAssignmentQuery(command.societyId()));
    }

    @Transactional
    public AutoAssignmentView setCategories(SetStaffCategoriesCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        if (!policy.mayManageAssignment(currentActor.actor(), societyId)) {
            throw new ActionNotAllowedException("Only the society admin can say who handles which complaints");
        }
        Set<ComplaintCategoryId> chosen = command.categoryIds().stream().map(ComplaintCategoryId::new)
                .collect(Collectors.toSet());
        for (ComplaintCategoryId category : chosen) {
            categories.findById(societyId, category)
                    .orElseThrow(() -> new ComplaintCategoryNotFoundException(societyId, category));
        }
        StaffSkills staffSkills = skills.find(societyId, new UserId(command.staffId()));
        staffSkills.handleOnly(chosen, clock.instant());
        skills.save(staffSkills);
        events.publish(staffSkills.pullDomainEvents());
        return view(new GetAutoAssignmentQuery(command.societyId()));
    }
}
