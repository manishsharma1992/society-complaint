package com.mycity.runwal.complaint.application.service;

import static com.mycity.runwal.complaint.application.service.TestActors.SOCIETY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.complaint.application.query.StaffPerformanceQuery;
import com.mycity.runwal.complaint.application.query.StaffPerformanceView;
import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * November 2026. Plumbing has a 24 h SLA. Staff 30 (Raju) and 31 (Sunil); admin 1 runs the society, admin 2 tower 7.
 * Raju: complaint A (tower 7) resolved 4 h after assignment, B (tower 7) resolved after 30 h (late), C (tower 3)
 * in progress and overdue. Sunil: D (tower 3) assigned, not started, not yet due. E was Raju's too, but it was
 * raised and resolved in October, so it counts neither in November nor in the backlog.
 */
class StaffReportServiceTest {

    private static final Instant NOV = Instant.parse("2026-10-31T18:30:00Z");
    private static final Instant DEC = Instant.parse("2026-11-30T18:30:00Z");
    private static final Instant NOW = Instant.parse("2026-11-20T06:00:00Z");
    private static final UserId RAJU = new UserId(30);
    private static final UserId SUNIL = new UserId(31);
    private static final UserId ADMIN = new UserId(1);
    private static final ComplaintCategory PLUMBING = new ComplaintCategory(new ComplaintCategoryId(1), SOCIETY,
            "Plumbing", Duration.ofHours(24), true);

    private final InMemoryComplaints complaints = new InMemoryComplaints();
    private final TestActors.Switchable caller = new TestActors.Switchable(TestActors.societyAdmin(1));
    private final StaffReportService service = new StaffReportService(complaints, new ComplaintAccessPolicy(), caller,
            Clock.fixed(NOW, ZoneOffset.UTC));

    StaffReportServiceTest() {
        Instant day = NOV.plus(Duration.ofDays(2));
        Complaint a = raise(7, day);
        a.assignTo(RAJU, ADMIN, day.plus(Duration.ofHours(1)));
        a.startWork(RAJU, day.plus(Duration.ofHours(2)));
        a.resolve(RAJU, day.plus(Duration.ofHours(5)), "Washer replaced");
        save(a);

        Complaint b = raise(7, day);
        b.assignTo(RAJU, ADMIN, day.plus(Duration.ofHours(2)));
        b.startWork(RAJU, day.plus(Duration.ofHours(4)));
        b.resolve(RAJU, day.plus(Duration.ofHours(32)), "Pipe replaced");
        b.close(new UserId(10), day.plus(Duration.ofHours(40)));
        save(b);

        Complaint c = raise(3, NOW.minus(Duration.ofDays(3)));
        c.assignTo(RAJU, ADMIN, NOW.minus(Duration.ofDays(3)));
        c.startWork(RAJU, NOW.minus(Duration.ofDays(2)));
        save(c);

        Complaint d = raise(3, NOW.minus(Duration.ofHours(2)));
        d.assignTo(SUNIL, ADMIN, NOW.minus(Duration.ofHours(1)));
        save(d);

        Complaint e = raise(7, NOV.minus(Duration.ofDays(5)));
        e.assignTo(RAJU, ADMIN, NOV.minus(Duration.ofDays(5)));
        e.startWork(RAJU, NOV.minus(Duration.ofDays(4)));
        e.resolve(RAJU, NOV.minus(Duration.ofDays(4)), "Done");
        save(e);

        save(raise(7, day)); // never assigned: counts for nobody
    }

    private Complaint raise(long towerId, Instant at) {
        Complaint complaint = Complaint.raise(complaints.nextId(), SOCIETY,
                ComplaintLocation.commonArea(new TowerId(towerId)), PLUMBING, new ComplaintTitle("Leak"), null,
                new UserId(10), at, new SlaPolicy());
        complaint.pullDomainEvents();
        return complaint;
    }

    private void save(Complaint complaint) {
        complaint.pullDomainEvents();
        complaints.save(complaint);
    }

    private StaffPerformanceQuery november(Long towerId) {
        return new StaffPerformanceQuery(1, NOV, DEC, towerId);
    }

    @Test
    void theSocietyAdminSeesEveryStaffMembersVolumeSpeedSlaAndBacklog() {
        assertThat(service.report(november(null))).containsExactly(
                new StaffPerformanceView(30, 3, 3, 2, 0, 120L, 1020L, 1, 1, 1),
                new StaffPerformanceView(31, 1, 0, 0, 0, null, null, 0, 1, 0));
    }

    @Test
    void aTowerAdminSeesOnlyTheirTower() {
        caller.become(TestActors.towerAdmin(2, 7));

        assertThat(service.report(november(7L))).containsExactly(
                new StaffPerformanceView(30, 2, 2, 2, 0, 90L, 1020L, 1, 0, 0));
        assertThatThrownBy(() -> service.report(november(3L))).isInstanceOf(ActionNotAllowedException.class);
        assertThatThrownBy(() -> service.report(november(null))).isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void staffSeeOnlyThemselves() {
        caller.become(TestActors.staff(31));

        assertThat(service.report(november(null))).extracting(StaffPerformanceView::staffId).containsExactly(31L);
    }

    @Test
    void residentsAndPendingUsersAreRefused() {
        caller.become(TestActors.resident(10));
        assertThatThrownBy(() -> service.report(november(null))).isInstanceOf(ActionNotAllowedException.class);
        caller.become(new Actor(new UserId(1), false, TestActors.societyAdmin(1).roles()));
        assertThatThrownBy(() -> service.report(november(null))).isInstanceOf(ActionNotAllowedException.class);
        caller.become(new Actor(new UserId(1), true, Set.of()));
        assertThatThrownBy(() -> service.report(november(null))).isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void thePeriodMustRunForwardAndBeAtMostAboutAYear() {
        assertThatThrownBy(() -> service.report(new StaffPerformanceQuery(1, DEC, NOV, null)))
                .isInstanceOf(InvalidComplaintException.class);
        assertThatThrownBy(() -> service.report(new StaffPerformanceQuery(1, NOV, NOV.plus(Duration.ofDays(401)), null)))
                .isInstanceOf(InvalidComplaintException.class);
    }

    @Test
    void readsEveryPageOfALargeSociety() {
        for (int i = 0; i < 450; i++) {
            Complaint complaint = raise(7, NOV.plus(Duration.ofDays(3)));
            complaint.assignTo(SUNIL, ADMIN, NOV.plus(Duration.ofDays(3)));
            save(complaint);
        }

        assertThat(service.report(november(null))).filteredOn(row -> row.staffId() == 31)
                .singleElement().satisfies(row -> {
                    assertThat(row.assigned()).isEqualTo(451);
                    assertThat(row.openNow()).isEqualTo(451);
                    assertThat(row.overdueNow()).isEqualTo(450);
                });
    }
}
