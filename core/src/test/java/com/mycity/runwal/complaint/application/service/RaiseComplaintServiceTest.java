package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.complaint.application.port.ComplaintsAddOn;
import java.util.HashSet;
import static com.mycity.runwal.complaint.application.service.TestActors.SOCIETY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.complaint.application.command.RaiseComplaintCommand;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.domain.event.ComplaintRaised;
import com.mycity.runwal.complaint.domain.exception.ComplaintCategoryNotFoundException;
import com.mycity.runwal.complaint.domain.exception.FlatNotFoundException;
import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryRepository;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RaiseComplaintServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-24T08:00:00Z");

    private final InMemoryComplaints complaints = new InMemoryComplaints();
    private final List<DomainEvent> published = new ArrayList<>();
    private final TestActors.Switchable caller = new TestActors.Switchable(TestActors.resident(10));
    private final TowersOff towersOff = new TowersOff();
    private final RaiseComplaintService service = new RaiseComplaintService(
            complaints,
            categories(new ComplaintCategory(new ComplaintCategoryId(5), SOCIETY, "Lift", Duration.ofHours(4), true)),
            TestActors.flats(Map.of(1204L, 3L, 1205L, 3L), Map.of(10L, Set.of(1204L))),
            new SlaPolicy(),
            new ComplaintAccessPolicy(),
            caller,
            published::addAll,
            new FakePhotoStorage(),
            towersOff,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void residentRaisesComplaintForOwnFlatWithTowerFromResidency() {
        ComplaintView view = service.raise(command("FLAT", 1204L, null, 5));

        assertThat(view.status()).isEqualTo("OPEN");
        assertThat(view.raisedBy()).isEqualTo(10);
        assertThat(view.towerId()).isEqualTo(3L);
        assertThat(view.flatId()).isEqualTo(1204L);
        assertThat(view.dueAt()).isEqualTo(NOW.plus(Duration.ofHours(4)));
        assertThat(complaints.stored).containsKey(view.id());
        assertThat(published).singleElement().isInstanceOf(ComplaintRaised.class);
    }

    @Test
    void residentCannotRaiseForSomeoneElsesFlat() {
        assertThatThrownBy(() -> service.raise(command("FLAT", 1205L, null, 5)))
                .isInstanceOf(ActionNotAllowedException.class);
        assertThat(complaints.stored).isEmpty();
    }

    @Test
    void adminMayRaiseForAnyFlat() {
        caller.become(TestActors.societyAdmin(1));

        assertThat(service.raise(command("FLAT", 1205L, null, 5)).raisedBy()).isEqualTo(1);
    }

    @Test
    void anyoneInTheSocietyMayReportACommonArea() {
        ComplaintView view = service.raise(command("COMMON_AREA", null, 3L, 5));

        assertThat(view.scope()).isEqualTo("COMMON_AREA");
        assertThat(view.towerId()).isEqualTo(3L);
        assertThat(view.flatId()).isNull();
    }

    @Test
    void pendingOrForeignUsersCannotRaise() {
        caller.become(new Actor(new UserId(100), false, TestActors.resident(100).roles()));
        assertThatThrownBy(() -> service.raise(command("COMMON_AREA", null, 3L, 5)))
                .isInstanceOf(ActionNotAllowedException.class);

        caller.become(new Actor(new UserId(101), true, Set.of()));
        assertThatThrownBy(() -> service.raise(command("COMMON_AREA", null, 3L, 5)))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void unknownFlatIsRejected() {
        assertThatThrownBy(() -> service.raise(command("FLAT", 999L, null, 5)))
                .isInstanceOf(FlatNotFoundException.class);
        assertThat(complaints.stored).isEmpty();
        assertThat(published).isEmpty();
    }

    @Test
    void flatComplaintWithoutFlatIsRejected() {
        assertThatThrownBy(() -> service.raise(command("FLAT", null, null, 5)))
                .isInstanceOf(InvalidComplaintException.class);
    }

    @Test
    void unknownCategoryIsRejected() {
        assertThatThrownBy(() -> service.raise(command("COMMON_AREA", null, null, 77)))
                .isInstanceOf(ComplaintCategoryNotFoundException.class);
    }

    private static RaiseComplaintCommand command(String scope, Long flatId, Long towerId, long categoryId) {
        return new RaiseComplaintCommand(1, scope, flatId, towerId, categoryId, "Lift stuck", null);
    }

    static ComplaintCategoryRepository categories(ComplaintCategory... categories) {
        return new ComplaintCategoryRepository() {
            @Override
            public Optional<ComplaintCategory> findById(SocietyId societyId,
                                                                  ComplaintCategoryId id) {
                return List.of(categories).stream().filter(c -> c.id().equals(id) && c.belongsTo(societyId)).findFirst();
            }

            @Override
            public List<ComplaintCategory> findActiveBySociety(SocietyId societyId) {
                return List.of(categories).stream().filter(c -> c.belongsTo(societyId) && c.isActive()).toList();
            }
        };
    }

    @Test
    void aTowerThatHandlesComplaintsItsOwnWayGetsNoNewOnes() {
        towersOff.ids.add(3L);

        assertThatThrownBy(() -> service.raise(command("FLAT", 1204L, null, 5)))
                .isInstanceOf(ActionNotAllowedException.class).hasMessageContaining("outside this app");
        assertThatThrownBy(() -> service.raise(command("COMMON_AREA", null, 3L, 5)))
                .isInstanceOf(ActionNotAllowedException.class);
        assertThat(service.raise(command("COMMON_AREA", null, null, 5)).scope())
                .as("society-wide areas such as the clubhouse stay open").isEqualTo("COMMON_AREA");
        assertThat(service.raise(command("COMMON_AREA", null, 4L, 5)).towerId()).isEqualTo(4L);
    }

    /** The Complaints add-on is on for the society, and off for the towers listed here. */
    private static final class TowersOff implements ComplaintsAddOn {

        private final Set<Long> ids = new HashSet<>();

        @Override
        public boolean isOnFor(SocietyId societyId) {
            return true;
        }

        @Override
        public boolean isOnIn(SocietyId societyId, TowerId towerId) {
            return !ids.contains(towerId.value());
        }
    }
}
