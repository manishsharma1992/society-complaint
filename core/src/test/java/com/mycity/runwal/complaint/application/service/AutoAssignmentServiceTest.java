package com.mycity.runwal.complaint.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.complaint.application.command.SetStaffCategoriesCommand;
import com.mycity.runwal.complaint.application.command.SwitchAutoAssignmentCommand;
import com.mycity.runwal.complaint.application.port.ComplaintsAddOn;
import com.mycity.runwal.complaint.application.query.AutoAssignmentView;
import com.mycity.runwal.complaint.application.query.GetAutoAssignmentQuery;
import com.mycity.runwal.complaint.domain.event.ComplaintAssigned;
import com.mycity.runwal.complaint.domain.exception.ComplaintCategoryNotFoundException;
import com.mycity.runwal.complaint.domain.model.assignment.AutoAssignment;
import com.mycity.runwal.complaint.domain.model.assignment.AutoAssignmentRepository;
import com.mycity.runwal.complaint.domain.model.assignment.StaffMember;
import com.mycity.runwal.complaint.domain.model.assignment.StaffSkills;
import com.mycity.runwal.complaint.domain.model.assignment.StaffSkillsRepository;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.complaint.domain.service.AssignmentPlanner;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AutoAssignmentServiceTest {

    private static final SocietyId SOCIETY = TestActors.SOCIETY;
    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");
    private static final ComplaintCategory PLUMBING =
            new ComplaintCategory(new ComplaintCategoryId(1), SOCIETY, "Plumbing", Duration.ofHours(24), true);
    private static final ComplaintCategory LIFT =
            new ComplaintCategory(new ComplaintCategoryId(3), SOCIETY, "Lift", Duration.ofHours(4), true);
    private static final UserId RAJU = new UserId(30);
    private static final UserId SUNIL = new UserId(31);
    private static final UserId TOWER_MANAGER = new UserId(2);

    private final InMemoryComplaints complaints = new InMemoryComplaints();
    private final Settings settings = new Settings();
    private final Skills skills = new Skills();
    private final Set<UserId> onDuty = new HashSet<>();
    private final List<DomainEvent> published = new ArrayList<>();
    private final TestActors.Switchable caller = new TestActors.Switchable(TestActors.societyAdmin(1));
    private boolean addOnOn = true;

    private final AutoAssignmentService service = new AutoAssignmentService(settings, skills, complaints,
            RaiseComplaintServiceTest.categories(PLUMBING, LIFT), new AddOn(), society -> Set.copyOf(onDuty),
            society -> List.of(new StaffMember(RAJU, Set.of(), false), new StaffMember(SUNIL, Set.of(), false),
                    new StaffMember(TOWER_MANAGER, Set.of(new TowerId(3)), true)),
            new AssignmentPlanner(), new ComplaintAccessPolicy(), caller, published::addAll,
            Clock.fixed(NOW, ZoneOffset.UTC), new NoTransactions());

    private Complaint waiting(ComplaintCategory category) {
        Complaint complaint = Complaint.raise(complaints.nextId(), SOCIETY,
                ComplaintLocation.flat(new TowerId(3), new FlatId(1204)), category, new ComplaintTitle("Problem"), null,
                new UserId(10), NOW.minus(Duration.ofMinutes(5)), new SlaPolicy());
        complaint.pullDomainEvents();
        complaints.save(complaint);
        return complaint;
    }

    @Test
    void aFreeStaffMemberOnShiftGetsTheWaitingComplaintOfTheirCategory() {
        settings.on = true;
        skills.set(RAJU, PLUMBING);
        onDuty.add(RAJU);
        Complaint leak = waiting(PLUMBING);

        assertThat(service.assignWaiting(SOCIETY)).isEqualTo(1);
        assertThat(leak.status()).isEqualTo(ComplaintStatus.ASSIGNED);
        assertThat(leak.assignedTo()).contains(RAJU);
        assertThat(published).containsExactly(new ComplaintAssigned(leak.id(), RAJU, RAJU, NOW));
    }

    @Test
    void nothingIsAssignedWhileOffOffShiftBusyOrWithoutTheAddOn() {
        skills.set(RAJU, PLUMBING);
        onDuty.add(RAJU);
        Complaint leak = waiting(PLUMBING);
        assertThat(service.assignWaiting(SOCIETY)).as("switched off").isZero();

        settings.on = true;
        addOnOn = false;
        assertThat(service.assignWaiting(SOCIETY)).as("complaints add-on off").isZero();
        addOnOn = true;

        onDuty.clear();
        assertThat(service.assignWaiting(SOCIETY)).as("nobody on shift").isZero();
        onDuty.add(RAJU);

        Complaint other = waiting(PLUMBING);
        other.assignTo(RAJU, new UserId(1), NOW);
        assertThat(service.assignWaiting(SOCIETY)).as("Raju already has a job").isZero();
        assertThat(leak.status()).isEqualTo(ComplaintStatus.OPEN);

        other.startWork(RAJU, NOW);
        other.resolve(RAJU, NOW, "Fixed");
        assertThat(service.assignWaiting(SOCIETY)).as("free again").isEqualTo(1);
        assertThat(leak.assignedTo()).contains(RAJU);
    }

    @Test
    void liftsGoToTheTowerManagerAtAnyHourEvenWithOtherLiftsOpen() {
        settings.on = true;
        skills.set(TOWER_MANAGER, LIFT);
        Complaint first = waiting(LIFT);
        Complaint second = waiting(LIFT);

        assertThat(service.assignWaiting(SOCIETY)).as("no shift needed, no one-job limit").isEqualTo(2);
        assertThat(first.assignedTo()).contains(TOWER_MANAGER);
        assertThat(second.assignedTo()).contains(TOWER_MANAGER);
    }

    @Test
    void theSocietyAdminSwitchesItAndSaysWhoHandlesWhat() {
        AutoAssignmentView on = service.switchOn(new SwitchAutoAssignmentCommand(1, true));
        assertThat(on.on()).isTrue();

        AutoAssignmentView view = service.setCategories(new SetStaffCategoriesCommand(1, 31, List.of(3L, 1L)));
        assertThat(view.staff()).containsExactly(new AutoAssignmentView.StaffCategoriesView(31, List.of(1L, 3L)));
        assertThatThrownBy(() -> service.setCategories(new SetStaffCategoriesCommand(1, 31, List.of(99L))))
                .isInstanceOf(ComplaintCategoryNotFoundException.class);
    }

    @Test
    void onlyTheSocietyAdminChangesItAndResidentsOnlyLearnWhetherItIsOn() {
        settings.on = true;
        skills.set(RAJU, PLUMBING);
        for (var notAdmin : List.of(TestActors.towerAdmin(2, 3), TestActors.staff(30), TestActors.resident(10))) {
            caller.become(notAdmin);
            assertThatThrownBy(() -> service.switchOn(new SwitchAutoAssignmentCommand(1, false)))
                    .isInstanceOf(ActionNotAllowedException.class);
            assertThatThrownBy(() -> service.setCategories(new SetStaffCategoriesCommand(1, 30, List.of())))
                    .isInstanceOf(ActionNotAllowedException.class);
        }
        caller.become(TestActors.resident(10));
        assertThat(service.view(new GetAutoAssignmentQuery(1))).isEqualTo(new AutoAssignmentView(true, List.of()));
        caller.become(TestActors.towerAdmin(2, 3));
        assertThat(service.view(new GetAutoAssignmentQuery(1)).staff()).hasSize(1);
    }

    private final class AddOn implements ComplaintsAddOn {
        @Override
        public boolean isOnFor(SocietyId societyId) {
            return addOnOn;
        }

        @Override
        public boolean isOnIn(SocietyId societyId, TowerId towerId) {
            return addOnOn;
        }
    }

    private static final class Settings implements AutoAssignmentRepository {
        boolean on;

        @Override
        public AutoAssignment find(SocietyId societyId) {
            return new AutoAssignment(societyId, on);
        }

        @Override
        public void save(AutoAssignment autoAssignment) {
            on = autoAssignment.isOn();
        }
    }

    private static final class Skills implements StaffSkillsRepository {
        private final Map<UserId, StaffSkills> byStaff = new HashMap<>();

        void set(UserId staff, ComplaintCategory... categories) {
            Set<ComplaintCategoryId> ids = new HashSet<>();
            for (ComplaintCategory c : categories) {
                ids.add(c.id());
            }
            byStaff.put(staff, new StaffSkills(SOCIETY, staff, ids));
        }

        @Override
        public List<StaffSkills> findBySociety(SocietyId societyId) {
            return byStaff.values().stream().filter(s -> !s.categories().isEmpty()).toList();
        }

        @Override
        public StaffSkills find(SocietyId societyId, UserId staffId) {
            return byStaff.getOrDefault(staffId, StaffSkills.none(societyId, staffId));
        }

        @Override
        public void save(StaffSkills staffSkills) {
            byStaff.put(staffSkills.staffId(), staffSkills);
        }
    }

    /** Runs the work straight away: the in-memory stores need no transactions. */
    private static final class NoTransactions implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
