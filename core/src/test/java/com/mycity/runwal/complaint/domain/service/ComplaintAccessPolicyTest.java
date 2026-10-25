package com.mycity.runwal.complaint.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.actor.ActorRole;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ComplaintAccessPolicyTest {

    private static final SocietyId RUNWAL = new SocietyId(1);
    private static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");
    private static final ComplaintLocation B2_503 = ComplaintLocation.flat(new TowerId(7), new FlatId(667));
    private static final Set<FlatId> LIVES_IN_503 = Set.of(new FlatId(667));

    private final ComplaintAccessPolicy policy = new ComplaintAccessPolicy();

    private static Actor actor(long id, ActorRole.Part part, Long tower) {
        return new Actor(new UserId(id), true, Set.of(new ActorRole(part, RUNWAL, tower == null ? null : new TowerId(tower))));
    }

    private static Complaint complaint(ComplaintLocation location, long raisedBy) {
        ComplaintCategory plumbing = new ComplaintCategory(new ComplaintCategoryId(1), RUNWAL, "Plumbing",
                Duration.ofHours(24), true);
        return Complaint.raise(new ComplaintId(1), RUNWAL, location, plumbing, new ComplaintTitle("Leak"), null,
                new UserId(raisedBy), NOW, new SlaPolicy());
    }

    @Test
    void raising() {
        Actor resident = actor(10, ActorRole.Part.RESIDENT, null);
        ComplaintLocation b2504 = ComplaintLocation.flat(new TowerId(7), new FlatId(668));

        assertThat(policy.mayRaise(resident, RUNWAL, B2_503, LIVES_IN_503)).isTrue();
        assertThat(policy.mayRaise(resident, RUNWAL, b2504, LIVES_IN_503)).isFalse();
        assertThat(policy.mayRaise(resident, RUNWAL, ComplaintLocation.commonArea(null), Set.of())).isTrue();
        assertThat(policy.mayRaise(resident, new SocietyId(2), ComplaintLocation.commonArea(null), Set.of())).isFalse();
        assertThat(policy.mayRaise(actor(3, ActorRole.Part.STAFF, null), RUNWAL, b2504, Set.of())).isTrue();
        assertThat(policy.mayRaise(new Actor(new UserId(10), false, resident.roles()), RUNWAL, B2_503, LIVES_IN_503))
                .isFalse();
    }

    @Test
    void managersAreTheSocietyAdminAndThisTowersAdmin() {
        Complaint inB2 = complaint(B2_503, 10);

        assertThat(policy.mayAssignOrReject(actor(1, ActorRole.Part.SOCIETY_ADMIN, null), inB2)).isTrue();
        assertThat(policy.mayAssignOrReject(actor(2, ActorRole.Part.TOWER_ADMIN, 7L), inB2)).isTrue();
        assertThat(policy.mayAssignOrReject(actor(5, ActorRole.Part.TOWER_ADMIN, 8L), inB2)).isFalse();
        assertThat(policy.mayAssignOrReject(actor(3, ActorRole.Part.STAFF, null), inB2)).isFalse();

        Complaint societyWide = complaint(ComplaintLocation.commonArea(null), 10);
        assertThat(policy.mayAssignOrReject(actor(2, ActorRole.Part.TOWER_ADMIN, 7L), societyWide)).isFalse();
        assertThat(policy.mayAssignOrReject(actor(1, ActorRole.Part.SOCIETY_ADMIN, null), societyWide)).isTrue();
    }

    @Test
    void assigneeWorksRaiserClosesAndBothAddPhotos() {
        Complaint complaint = complaint(B2_503, 10);
        complaint.assignTo(new UserId(3), new UserId(2), NOW);
        Actor assignee = actor(3, ActorRole.Part.STAFF, null);
        Actor otherStaff = actor(4, ActorRole.Part.STAFF, null);
        Actor raiser = actor(10, ActorRole.Part.RESIDENT, null);

        assertThat(policy.mayWorkOn(assignee, complaint)).isTrue();
        assertThat(policy.mayWorkOn(otherStaff, complaint)).isFalse();
        assertThat(policy.mayWorkOn(raiser, complaint)).isFalse();
        assertThat(policy.mayClose(raiser, complaint)).isTrue();
        assertThat(policy.mayClose(assignee, complaint)).isFalse();
        assertThat(policy.mayAddPhoto(raiser, complaint)).isTrue();
        assertThat(policy.mayAddPhoto(assignee, complaint)).isTrue();
        assertThat(policy.mayAddPhoto(otherStaff, complaint)).isFalse();
    }

    @Test
    void seeing() {
        Complaint neighbours = complaint(ComplaintLocation.flat(new TowerId(7), new FlatId(668)), 11);
        Actor resident = actor(10, ActorRole.Part.RESIDENT, null);

        assertThat(policy.maySee(resident, complaint(B2_503, 12), LIVES_IN_503)).isTrue();
        assertThat(policy.maySee(resident, neighbours, LIVES_IN_503)).isFalse();
        assertThat(policy.maySee(resident, complaint(ComplaintLocation.commonArea(null), 11), LIVES_IN_503)).isTrue();
        assertThat(policy.maySee(actor(3, ActorRole.Part.STAFF, null), neighbours, Set.of())).isTrue();
        assertThat(policy.maySeeAll(actor(3, ActorRole.Part.STAFF, null), RUNWAL)).isTrue();
        assertThat(policy.maySeeAll(resident, RUNWAL)).isFalse();
    }
}
