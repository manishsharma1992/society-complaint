package com.mycity.runwal.complaint.application.service;

import static com.mycity.runwal.complaint.application.service.TestActors.SOCIETY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.complaint.application.command.AssignComplaintCommand;
import com.mycity.runwal.complaint.application.command.CloseComplaintCommand;
import com.mycity.runwal.complaint.application.command.RejectComplaintCommand;
import com.mycity.runwal.complaint.application.command.ResolveComplaintCommand;
import com.mycity.runwal.complaint.application.command.StartWorkCommand;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.application.query.StatusChangeView;
import com.mycity.runwal.complaint.domain.event.ComplaintAssigned;
import com.mycity.runwal.complaint.domain.event.ComplaintClosed;
import com.mycity.runwal.complaint.domain.event.ComplaintRejected;
import com.mycity.runwal.complaint.domain.event.ComplaintResolved;
import com.mycity.runwal.complaint.domain.event.ComplaintWorkStarted;
import com.mycity.runwal.complaint.domain.exception.InvalidStatusTransitionException;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Complaint 7 in B2 (tower 7), flat 667, raised by resident 10. */
class ComplaintLifecycleServiceTest {

    private static final Instant RAISED_AT = Instant.parse("2026-10-24T08:00:00Z");
    private static final Instant NOW = RAISED_AT.plus(Duration.ofHours(1));

    private final InMemoryComplaints complaints = new InMemoryComplaints();
    private final List<DomainEvent> published = new ArrayList<>();
    private final TestActors.Switchable caller = new TestActors.Switchable(TestActors.towerAdmin(2, 7));
    private final ComplaintLifecycleService service = new ComplaintLifecycleService(complaints,
            new ComplaintAccessPolicy(), caller, published::addAll, new FakePhotoStorage(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void storeOpenComplaint() {
        ComplaintCategory plumbing = new ComplaintCategory(new ComplaintCategoryId(1), SOCIETY, "Plumbing",
                Duration.ofHours(24), true);
        Complaint complaint = Complaint.raise(new ComplaintId(7), SOCIETY,
                ComplaintLocation.flat(new TowerId(7), new FlatId(667)), plumbing,
                new ComplaintTitle("Tap leaking"), null, new UserId(10), RAISED_AT, new SlaPolicy());
        complaint.pullDomainEvents();
        complaints.save(complaint);
        complaints.saves = 0;
    }

    @Test
    void towerAdminAssignsStaffWorksResidentCloses() {
        service.assign(new AssignComplaintCommand(1, 7, 3));
        caller.become(TestActors.staff(3));
        service.startWork(new StartWorkCommand(1, 7));
        service.resolve(new ResolveComplaintCommand(1, 7, "Washer replaced"));
        caller.become(TestActors.resident(10));
        ComplaintView view = service.close(new CloseComplaintCommand(1, 7)).orElseThrow();

        assertThat(view.status()).isEqualTo("CLOSED");
        assertThat(view.assignedTo()).isEqualTo(3L);
        assertThat(view.history()).extracting(StatusChangeView::changedBy).containsExactly(10L, 2L, 3L, 3L, 10L);
        assertThat(view.history().get(3).note()).isEqualTo("Washer replaced");
        assertThat(complaints.stored.get(7L).status()).isEqualTo(ComplaintStatus.CLOSED);
        assertThat(published).hasExactlyElementsOfTypes(ComplaintAssigned.class, ComplaintWorkStarted.class,
                ComplaintResolved.class, ComplaintClosed.class);
    }

    @Test
    void onlyManagersOfThisTowerAssignOrReject() {
        caller.become(TestActors.towerAdmin(5, 8));
        assertThatThrownBy(() -> service.assign(new AssignComplaintCommand(1, 7, 3)))
                .isInstanceOf(ActionNotAllowedException.class);
        caller.become(TestActors.resident(10));
        assertThatThrownBy(() -> service.reject(new RejectComplaintCommand(1, 7, "No")))
                .isInstanceOf(ActionNotAllowedException.class);
        caller.become(TestActors.staff(3));
        assertThatThrownBy(() -> service.assign(new AssignComplaintCommand(1, 7, 3)))
                .isInstanceOf(ActionNotAllowedException.class);

        caller.become(TestActors.societyAdmin(1));
        assertThat(service.reject(new RejectComplaintCommand(1, 7, "Duplicate of #5")).orElseThrow().status())
                .isEqualTo("REJECTED");
        assertThat(published).singleElement().isInstanceOf(ComplaintRejected.class);
    }

    @Test
    void onlyTheAssignedStaffMemberStartsWork() {
        service.assign(new AssignComplaintCommand(1, 7, 3));

        caller.become(TestActors.staff(4));
        assertThatThrownBy(() -> service.startWork(new StartWorkCommand(1, 7)))
                .isInstanceOf(ActionNotAllowedException.class)
                .hasMessage("Only the assigned staff member or an admin can start work");
        assertThat(complaints.stored.get(7L).status()).isEqualTo(ComplaintStatus.ASSIGNED);
    }

    @Test
    void anotherResidentCannotClose() {
        caller.become(TestActors.resident(11));

        assertThatThrownBy(() -> service.close(new CloseComplaintCommand(1, 7)))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void complaintOfAnotherSocietyIsNotFound() {
        assertThat(service.startWork(new StartWorkCommand(2, 7))).isEmpty();
        assertThat(complaints.stored.get(7L).status()).isEqualTo(ComplaintStatus.OPEN);
    }

    @Test
    void invalidTransitionChangesNothing() {
        assertThatThrownBy(() -> service.close(new CloseComplaintCommand(1, 7)))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(published).isEmpty();
        assertThat(complaints.saves).isZero();
    }
}
