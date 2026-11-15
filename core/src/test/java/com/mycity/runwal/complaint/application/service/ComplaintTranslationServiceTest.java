package com.mycity.runwal.complaint.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.complaint.application.query.ComplaintTranslationView;
import com.mycity.runwal.complaint.application.query.TranslateComplaintQuery;
import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Resident 10 lives in flat 667; complaint 1 is about flat 667, complaint 2 about flat 668 (raised by 11). */
class ComplaintTranslationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T06:00:00Z");

    private final InMemoryComplaints complaints = new InMemoryComplaints();
    private final TestActors.Switchable caller = new TestActors.Switchable(TestActors.resident(10));
    private final List<String> sent = new ArrayList<>();
    private final ComplaintTranslationService service = new ComplaintTranslationService(
            new ComplaintQueryService(complaints, new ComplaintAccessPolicy(), caller,
                    TestActors.flats(Map.of(), Map.of(10L, Set.of(667L))), new FakePhotoStorage()),
            (text, language) -> {
                sent.add(text);
                return "[" + language + "] " + text;
            });

    ComplaintTranslationServiceTest() {
        Complaint ownFlat = raise(667, "Kitchen tap leaking", 10);
        ownFlat.comment(new UserId(3), "नळ बदलला", NOW);
        complaints.save(ownFlat);
        raise(668, "Neighbour's leak", 11);
    }

    private Complaint raise(long flat, String description, long raisedBy) {
        ComplaintCategory plumbing = new ComplaintCategory(new ComplaintCategoryId(1), new SocietyId(1), "Plumbing",
                Duration.ofHours(24), true);
        Complaint complaint = Complaint.raise(complaints.nextId(), new SocietyId(1),
                ComplaintLocation.flat(new TowerId(7), new FlatId(flat)), plumbing,
                new ComplaintTitle("Water leak"), description, new UserId(raisedBy), NOW, new SlaPolicy());
        complaints.save(complaint);
        return complaint;
    }

    @Test
    void translatesTheTitleDescriptionAndEveryComment() {
        ComplaintTranslationView view = service.translate(new TranslateComplaintQuery(1, 1, "mr")).orElseThrow();

        assertThat(view.language()).isEqualTo("mr");
        assertThat(view.title()).isEqualTo("[mr] Water leak");
        assertThat(view.description()).isEqualTo("[mr] Kitchen tap leaking");
        assertThat(view.comments()).containsExactly(new ComplaintTranslationView.TranslatedCommentView(1, "[mr] नळ बदलला"));
    }

    @Test
    void onlyThoseWhoMaySeeTheComplaintGetItTranslated() {
        assertThat(service.translate(new TranslateComplaintQuery(1, 2, "mr"))).isEmpty();
        assertThat(service.translate(new TranslateComplaintQuery(1, 99, "mr"))).isEmpty();
        assertThat(sent).isEmpty();
    }

    @Test
    void anUnknownLanguageIsRefusedBeforeAnythingIsSent() {
        assertThatThrownBy(() -> service.translate(new TranslateComplaintQuery(1, 1, "fr")))
                .isInstanceOf(InvalidComplaintException.class);
        assertThat(sent).isEmpty();
    }
}
