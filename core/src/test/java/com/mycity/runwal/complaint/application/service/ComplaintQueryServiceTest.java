package com.mycity.runwal.complaint.application.service;

import static com.mycity.runwal.complaint.application.service.TestActors.SOCIETY;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.application.query.GetComplaintQuery;
import com.mycity.runwal.complaint.application.query.ListComplaintsQuery;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Resident 10 lives in flat 667. Complaints: 1 = flat 667 raised by family member 12, 2 = flat 668 by 11,
 * 3 = common area by 11, 4 = flat 668 raised by 10 (e.g. reported for a neighbour).
 */
class ComplaintQueryServiceTest {

    private final InMemoryComplaints complaints = new InMemoryComplaints();
    private final TestActors.Switchable caller = new TestActors.Switchable(TestActors.resident(10));
    private final ComplaintQueryService service = new ComplaintQueryService(complaints, new ComplaintAccessPolicy(),
            caller, TestActors.flats(Map.of(), Map.of(10L, Set.of(667L), 12L, Set.of(667L))), new FakePhotoStorage());

    ComplaintQueryServiceTest() {
        raise(ComplaintLocation.flat(new TowerId(7), new FlatId(667)), 12);
        raise(ComplaintLocation.flat(new TowerId(7), new FlatId(668)), 11);
        raise(ComplaintLocation.commonArea(new TowerId(7)), 11);
        raise(ComplaintLocation.flat(new TowerId(7), new FlatId(668)), 10);
    }

    @Test
    void residentSeesOwnFlatOwnRaisedAndCommonAreaComplaints() {
        assertThat(service.list(new ListComplaintsQuery(1, null, null, null, 0, 20)))
                .extracting(ComplaintView::id).containsExactly(1L, 3L, 4L);
        assertThat(service.get(new GetComplaintQuery(1, 2))).isEmpty();
        assertThat(service.get(new GetComplaintQuery(1, 1))).isPresent();
    }

    @Test
    void adminsAndStaffSeeEverything() {
        caller.become(TestActors.staff(3));
        assertThat(service.list(new ListComplaintsQuery(1, null, null, null, 0, 20))).hasSize(4);
        caller.become(TestActors.towerAdmin(2, 7));
        assertThat(service.get(new GetComplaintQuery(1, 2))).isPresent();
    }

    @Test
    void staffListTheirOwnJobs() {
        Complaint commonArea = complaints.findById(SOCIETY, new ComplaintId(3)).orElseThrow();
        commonArea.assignTo(new UserId(3), new UserId(1), Instant.parse("2026-10-24T09:00:00Z"));
        complaints.save(commonArea);
        caller.become(TestActors.staff(3));

        assertThat(service.list(new ListComplaintsQuery(1, null, null, 3L, 0, 20)))
                .extracting(ComplaintView::id).containsExactly(3L);
        assertThat(service.list(new ListComplaintsQuery(1, null, null, 30L, 0, 20))).isEmpty();
    }

    @Test
    void pendingUsersAndOtherSocietiesSeeNothing() {
        caller.become(new Actor(new UserId(100), false, TestActors.resident(100).roles()));
        assertThat(service.list(new ListComplaintsQuery(1, null, null, null, 0, 20))).isEmpty();
        assertThat(service.get(new GetComplaintQuery(1, 3))).isEmpty();

        caller.become(new Actor(new UserId(200), true, Set.of()));
        assertThat(service.list(new ListComplaintsQuery(1, null, null, null, 0, 20))).isEmpty();
    }

    private void raise(ComplaintLocation location, long raisedBy) {
        ComplaintCategory plumbing = new ComplaintCategory(new ComplaintCategoryId(1), SOCIETY, "Plumbing",
                Duration.ofHours(24), true);
        complaints.save(Complaint.raise(complaints.nextId(), SOCIETY, location, plumbing, new ComplaintTitle("Leak"),
                null, new UserId(raisedBy), Instant.parse("2026-10-24T08:00:00Z"), new SlaPolicy()));
    }
}
