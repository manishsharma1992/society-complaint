package com.mycity.runwal.complaint.application.service;

import static com.mycity.runwal.complaint.application.service.TestActors.SOCIETY;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.complaint.application.command.AddCommentCommand;
import com.mycity.runwal.complaint.application.query.CommentView;
import com.mycity.runwal.complaint.domain.event.ComplaintCommented;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Complaint 1: flat 667 (resident 10). Complaint 2: common area. */
class ComplaintCommentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");

    private final InMemoryComplaints complaints = new InMemoryComplaints();
    private final List<DomainEvent> published = new ArrayList<>();
    private final TestActors.Switchable caller = new TestActors.Switchable(TestActors.resident(10));
    private final ComplaintCommentService service = new ComplaintCommentService(complaints,
            new ComplaintAccessPolicy(), caller,
            TestActors.flats(Map.of(), Map.of(10L, Set.of(667L))), published::addAll, new FakePhotoStorage(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    ComplaintCommentServiceTest() {
        raise(ComplaintLocation.flat(new TowerId(7), new FlatId(667)), 10);
        raise(ComplaintLocation.commonArea(new TowerId(7)), 11);
    }

    @Test
    void residentAndStaffTalkAboutTheComplaint() {
        service.comment(new AddCommentCommand(1, 1, "Leak got worse"));
        caller.become(TestActors.staff(3));
        List<CommentView> comments = service.comment(new AddCommentCommand(1, 1, "Plumber at 4 pm"))
                .orElseThrow().comments();

        assertThat(comments).extracting(CommentView::authorId).containsExactly(10L, 3L);
        assertThat(comments.get(1).text()).isEqualTo("Plumber at 4 pm");
        assertThat(comments.get(1).postedAt()).isEqualTo(NOW);
        assertThat(published).hasSize(2).allMatch(e -> e instanceof ComplaintCommented);
    }

    @Test
    void neighbourCanAddToACommonAreaComplaint() {
        assertThat(service.comment(new AddCommentCommand(1, 2, "Same in my wing"))).isPresent();
    }

    @Test
    void complaintsYouCannotSeeAreNotFound() {
        caller.become(TestActors.resident(12));
        assertThat(service.comment(new AddCommentCommand(1, 1, "Hello"))).isEmpty();
        assertThat(service.comment(new AddCommentCommand(2, 1, "Hello"))).isEmpty();
        assertThat(complaints.stored.get(1L).comments()).isEmpty();
    }

    private void raise(ComplaintLocation location, long raisedBy) {
        ComplaintCategory plumbing = new ComplaintCategory(new ComplaintCategoryId(1), SOCIETY, "Plumbing",
                Duration.ofHours(24), true);
        Complaint complaint = Complaint.raise(complaints.nextId(), SOCIETY, location, plumbing,
                new ComplaintTitle("Leak"), null, new UserId(raisedBy), NOW, new SlaPolicy());
        complaint.pullDomainEvents();
        complaints.save(complaint);
    }
}
