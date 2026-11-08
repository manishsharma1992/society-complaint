package com.mycity.runwal.complaint.domain.model.complaint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.complaint.domain.event.ComplaintAssigned;
import com.mycity.runwal.complaint.domain.event.ComplaintClosed;
import com.mycity.runwal.complaint.domain.event.ComplaintCommented;
import com.mycity.runwal.complaint.domain.event.ComplaintRaised;
import com.mycity.runwal.complaint.domain.event.ComplaintRejected;
import com.mycity.runwal.complaint.domain.event.ComplaintResolved;
import com.mycity.runwal.complaint.domain.event.ComplaintWorkStarted;
import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.complaint.domain.exception.InvalidStatusTransitionException;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ComplaintTest {

    private static final SocietyId SOCIETY = new SocietyId(1);
    private static final UserId RESIDENT = new UserId(10);
    private static final UserId ADMIN = new UserId(20);
    private static final UserId STAFF = new UserId(30);
    private static final Instant RAISED_AT = Instant.parse("2026-10-24T08:00:00Z");
    private static final ComplaintCategory PLUMBING =
            new ComplaintCategory(new ComplaintCategoryId(1), SOCIETY, "Plumbing", Duration.ofHours(24), true);
    private static final ComplaintLocation FLAT_1204 = ComplaintLocation.flat(new TowerId(3), new FlatId(1204));

    private static Complaint raise(ComplaintCategory category) {
        return Complaint.raise(new ComplaintId(1), SOCIETY, FLAT_1204, category,
                new ComplaintTitle("  Kitchen tap leaking  "), "Since yesterday", RESIDENT, RAISED_AT, new SlaPolicy());
    }

    private static Complaint raisedComplaint() {
        Complaint complaint = raise(PLUMBING);
        complaint.pullDomainEvents();
        return complaint;
    }

    @Nested
    class Raising {

        @Test
        void startsOpenWithSlaDeadlineAndFirstHistoryEntry() {
            Complaint complaint = raise(PLUMBING);

            assertThat(complaint.status()).isEqualTo(ComplaintStatus.OPEN);
            assertThat(complaint.title().value()).isEqualTo("Kitchen tap leaking");
            assertThat(complaint.dueAt()).isEqualTo(RAISED_AT.plus(Duration.ofHours(24)));
            assertThat(complaint.assignedTo()).isEmpty();
            assertThat(complaint.history()).containsExactly(
                    new StatusChangeSnapshot(1, null, ComplaintStatus.OPEN, RESIDENT, RAISED_AT, null));
        }

        @Test
        void raisesComplaintRaisedEvent() {
            Complaint complaint = raise(PLUMBING);

            assertThat(complaint.pullDomainEvents()).containsExactly(new ComplaintRaised(complaint.id(), SOCIETY,
                    PLUMBING.id(), RESIDENT, RAISED_AT.plus(Duration.ofHours(24)), RAISED_AT));
            assertThat(complaint.pullDomainEvents()).isEmpty();
        }

        @Test
        void rejectsInactiveCategory() {
            ComplaintCategory retired =
                    new ComplaintCategory(new ComplaintCategoryId(2), SOCIETY, "Old", Duration.ofHours(1), false);

            assertThatThrownBy(() -> raise(retired))
                    .isInstanceOf(InvalidComplaintException.class)
                    .hasMessageContaining("no longer accepts complaints");
        }

        @Test
        void rejectsCategoryOfAnotherSociety() {
            ComplaintCategory foreign = new ComplaintCategory(
                    new ComplaintCategoryId(3), new SocietyId(2), "Plumbing", Duration.ofHours(24), true);

            assertThatThrownBy(() -> raise(foreign))
                    .isInstanceOf(InvalidComplaintException.class)
                    .hasMessageContaining("does not belong to society 1");
        }

        @Test
        void rejectsTooLongDescription() {
            assertThatThrownBy(() -> Complaint.raise(new ComplaintId(1), SOCIETY, FLAT_1204, PLUMBING,
                    new ComplaintTitle("Leak"), "x".repeat(Complaint.MAX_DESCRIPTION_LENGTH + 1),
                    RESIDENT, RAISED_AT, new SlaPolicy()))
                    .isInstanceOf(InvalidComplaintException.class);
        }
    }

    /** A photo uploaded for complaint 1 of society 1. */
    private static ComplaintPhoto photo(String fileName) {
        return new ComplaintPhoto("complaints/1/1/" + fileName, "image/jpeg", 200_000, RESIDENT, RAISED_AT);
    }

    @Nested
    class Comments {

        @Test
        void conversationIsKeptInOrderAndRaisesEvents() {
            Complaint complaint = raisedComplaint();

            complaint.comment(RESIDENT, "  Water is dripping on the floor ", RAISED_AT);
            complaint.comment(STAFF, "Plumber will come at 4 pm", RAISED_AT.plusSeconds(60));

            assertThat(complaint.comments()).containsExactly(
                    new ComplaintComment(1, RESIDENT, "Water is dripping on the floor", RAISED_AT),
                    new ComplaintComment(2, STAFF, "Plumber will come at 4 pm", RAISED_AT.plusSeconds(60)));
            assertThat(complaint.pullDomainEvents()).containsExactly(
                    new ComplaintCommented(complaint.id(), RESIDENT, 1, RAISED_AT),
                    new ComplaintCommented(complaint.id(), STAFF, 2, RAISED_AT.plusSeconds(60)));
        }

        @Test
        void closedOrRejectedComplaintTakesNoComments() {
            Complaint rejected = raisedComplaint();
            rejected.reject(ADMIN, RAISED_AT, "Duplicate");

            assertThatThrownBy(() -> rejected.comment(RESIDENT, "Why?", RAISED_AT))
                    .isInstanceOf(InvalidComplaintException.class)
                    .hasMessage("Comments cannot be added to a REJECTED complaint");
        }

        @Test
        void atMostTwoHundredComments() {
            Complaint complaint = raisedComplaint();
            for (int i = 0; i < Complaint.MAX_COMMENTS; i++) {
                complaint.comment(RESIDENT, "ping " + i, RAISED_AT);
            }

            assertThatThrownBy(() -> complaint.comment(RESIDENT, "one more", RAISED_AT))
                    .isInstanceOf(InvalidComplaintException.class);
        }

        @Test
        void commentsListCannotBeChangedFromOutside() {
            assertThatThrownBy(() -> raisedComplaint().comments().add(new ComplaintComment(1, RESIDENT, "x", RAISED_AT)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class Photos {

        @Test
        void photosAreKeptInOrderUnderTheComplaintsPrefix() {
            Complaint complaint = raisedComplaint();

            complaint.addPhoto(photo("a.jpg"));
            complaint.addPhoto(photo("b.jpg"));

            assertThat(complaint.photoKeyPrefix()).isEqualTo("complaints/1/1/");
            assertThat(complaint.photos()).containsExactly(photo("a.jpg"), photo("b.jpg"));
            assertThat(complaint.pullDomainEvents()).isEmpty();
        }

        @Test
        void atMostFivePhotos() {
            Complaint complaint = raisedComplaint();
            for (int i = 1; i <= Complaint.MAX_PHOTOS; i++) {
                complaint.addPhoto(photo(i + ".jpg"));
            }

            assertThatThrownBy(() -> complaint.addPhoto(photo("6.jpg")))
                    .isInstanceOf(InvalidComplaintException.class)
                    .hasMessage("A complaint can have at most 5 photos");
        }

        @Test
        void photoOfAnotherComplaintOrSocietyIsRefused() {
            Complaint complaint = raisedComplaint();

            for (String key : List.of("complaints/1/2/a.jpg", "complaints/2/1/a.jpg", "complaints/1/10/a.jpg")) {
                assertThatThrownBy(() -> complaint.addPhoto(
                        new ComplaintPhoto(key, "image/jpeg", 200_000, RESIDENT, RAISED_AT)))
                        .isInstanceOf(InvalidComplaintException.class)
                        .hasMessage("The photo was not uploaded for complaint 1");
            }
            assertThat(complaint.photos()).isEmpty();
        }

        @Test
        void samePhotoCannotBeAttachedTwice() {
            Complaint complaint = raisedComplaint();
            complaint.addPhoto(photo("a.jpg"));

            assertThatThrownBy(() -> complaint.addPhoto(photo("a.jpg")))
                    .isInstanceOf(InvalidComplaintException.class);
            assertThat(complaint.photos()).hasSize(1);
        }

        @Test
        void photosCanBeAddedWhileWorkIsOngoingButNotAfterClosing() {
            Complaint complaint = raisedComplaint();
            complaint.assignTo(STAFF, ADMIN, RAISED_AT);
            complaint.startWork(STAFF, RAISED_AT);
            complaint.addPhoto(photo("during-work.jpg"));
            complaint.resolve(STAFF, RAISED_AT, null);
            complaint.addPhoto(photo("after-fix.jpg"));
            complaint.close(RESIDENT, RAISED_AT);

            assertThatThrownBy(() -> complaint.addPhoto(photo("late.jpg")))
                    .isInstanceOf(InvalidComplaintException.class)
                    .hasMessage("Photos cannot be added to a CLOSED complaint");
        }

        @Test
        void rejectedComplaintTakesNoPhotos() {
            Complaint complaint = raisedComplaint();
            complaint.reject(ADMIN, RAISED_AT, "Duplicate");

            assertThatThrownBy(() -> complaint.addPhoto(photo("a.jpg")))
                    .isInstanceOf(InvalidComplaintException.class);
        }

        @Test
        void newPhotoKeyIsUnderTheComplaintsPrefixWithTheRightExtension() {
            Complaint complaint = raisedComplaint();

            assertThat(complaint.newPhotoKey("image/jpeg", 200_000, "u1")).isEqualTo("complaints/1/1/u1.jpg");
            assertThat(complaint.newPhotoKey("image/webp", 200_000, "u2")).isEqualTo("complaints/1/1/u2.webp");
            assertThat(complaint.photos()).isEmpty();
        }

        @Test
        void newPhotoKeyChecksTheSameRulesAsAttaching() {
            Complaint complaint = raisedComplaint();

            assertThatThrownBy(() -> complaint.newPhotoKey("image/gif", 200_000, "u1"))
                    .isInstanceOf(InvalidComplaintException.class);
            assertThatThrownBy(() -> complaint.newPhotoKey("image/jpeg", ComplaintPhoto.MAX_SIZE_BYTES + 1, "u1"))
                    .isInstanceOf(InvalidComplaintException.class);
            assertThatThrownBy(() -> complaint.newPhotoKey("image/jpeg", 200_000, "../x"))
                    .isInstanceOf(InvalidComplaintException.class);
            for (int i = 1; i <= Complaint.MAX_PHOTOS; i++) {
                complaint.addPhoto(photo(i + ".jpg"));
            }
            assertThatThrownBy(() -> complaint.newPhotoKey("image/jpeg", 200_000, "u6"))
                    .isInstanceOf(InvalidComplaintException.class)
                    .hasMessage("A complaint can have at most 5 photos");
        }

        @Test
        void photosListCannotBeChangedFromOutside() {
            Complaint complaint = raisedComplaint();

            assertThatThrownBy(() -> complaint.photos().add(photo("a.jpg")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void followsTheHappyPathAndRecordsEveryChange() {
            Complaint complaint = raisedComplaint();
            Instant t1 = RAISED_AT.plusSeconds(60);
            Instant t2 = RAISED_AT.plusSeconds(120);
            Instant t3 = RAISED_AT.plusSeconds(180);
            Instant t4 = RAISED_AT.plusSeconds(240);

            complaint.assignTo(STAFF, ADMIN, t1);
            complaint.startWork(STAFF, t2);
            complaint.resolve(STAFF, t3, "Washer replaced");
            complaint.close(RESIDENT, t4);

            assertThat(complaint.status()).isEqualTo(ComplaintStatus.CLOSED);
            assertThat(complaint.assignedTo()).contains(STAFF);
            assertThat(complaint.history()).extracting(StatusChangeSnapshot::to).containsExactly(
                    ComplaintStatus.OPEN, ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS,
                    ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED);
            assertThat(complaint.history().get(3))
                    .isEqualTo(new StatusChangeSnapshot(4, ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED,
                            STAFF, t3, "Washer replaced"));
            assertThat(complaint.pullDomainEvents()).containsExactly(
                    new ComplaintAssigned(complaint.id(), STAFF, ADMIN, t1),
                    new ComplaintWorkStarted(complaint.id(), STAFF, t2),
                    new ComplaintResolved(complaint.id(), SOCIETY, STAFF, t3),
                    new ComplaintClosed(complaint.id(), RESIDENT, t4));
        }

        @Test
        void canBeRejectedWithAReason() {
            Complaint complaint = raisedComplaint();

            complaint.reject(ADMIN, RAISED_AT, "  Duplicate of #12 ");

            assertThat(complaint.status()).isEqualTo(ComplaintStatus.REJECTED);
            assertThat(complaint.history().get(1).note()).isEqualTo("Duplicate of #12");
            assertThat(complaint.pullDomainEvents())
                    .containsExactly(new ComplaintRejected(complaint.id(), SOCIETY, ADMIN, "Duplicate of #12", RAISED_AT));
        }

        @Test
        void rejectingNeedsAReason() {
            Complaint complaint = raisedComplaint();

            assertThatThrownBy(() -> complaint.reject(ADMIN, RAISED_AT, " "))
                    .isInstanceOf(InvalidComplaintException.class);
            assertThat(complaint.status()).isEqualTo(ComplaintStatus.OPEN);
        }

        @Test
        void cannotSkipSteps() {
            Complaint complaint = raisedComplaint();

            assertThatThrownBy(() -> complaint.resolve(STAFF, RAISED_AT, null))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Complaint 1 cannot move from OPEN to RESOLVED");
            assertThat(complaint.history()).hasSize(1);
            assertThat(complaint.pullDomainEvents()).isEmpty();
        }

        @Test
        void closedComplaintIsFinal() {
            Complaint complaint = raisedComplaint();
            complaint.assignTo(STAFF, ADMIN, RAISED_AT);
            complaint.startWork(STAFF, RAISED_AT);
            complaint.resolve(STAFF, RAISED_AT, null);
            complaint.close(RESIDENT, RAISED_AT);

            assertThatThrownBy(() -> complaint.reject(ADMIN, RAISED_AT, "too late"))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void inProgressComplaintCannotBeRejected() {
            Complaint complaint = raisedComplaint();
            complaint.assignTo(STAFF, ADMIN, RAISED_AT);
            complaint.startWork(STAFF, RAISED_AT);

            assertThatThrownBy(() -> complaint.reject(ADMIN, RAISED_AT, "changed mind"))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Test
    void reconstitutedComplaintContinuesItsHistoryWithoutEvents() {
        Complaint original = raisedComplaint();
        original.assignTo(STAFF, ADMIN, RAISED_AT);

        Complaint restored = Complaint.reconstitute(original.id(), original.societyId(), original.location(),
                original.categoryId(), original.title(), original.description().orElse(null), original.raisedBy(),
                original.raisedAt(), original.dueAt(), original.status(), STAFF, original.history(),
                List.of(photo("a.jpg")),
                List.of(new ComplaintComment(1, RESIDENT, "Still leaking", RAISED_AT)));
        restored.startWork(STAFF, RAISED_AT);
        restored.comment(STAFF, "Coming at 4 pm", RAISED_AT);

        assertThat(restored.history()).extracting(StatusChangeSnapshot::sequence).containsExactly(1, 2, 3);
        assertThat(restored.pullDomainEvents()).containsExactly(new ComplaintWorkStarted(original.id(), STAFF, RAISED_AT),
                new ComplaintCommented(original.id(), STAFF, 2, RAISED_AT));
        assertThat(restored.photos()).containsExactly(photo("a.jpg"));
        assertThat(restored.comments()).extracting(ComplaintComment::sequence).containsExactly(1, 2);
    }

    @Test
    void theSystemAssignsAFreeStaffMemberAndTheHistorySaysSo() {
        Complaint complaint = raisedComplaint();
        Instant at = RAISED_AT.plus(Duration.ofMinutes(5));

        complaint.assignAutomatically(STAFF, at);

        assertThat(complaint.status()).isEqualTo(ComplaintStatus.ASSIGNED);
        assertThat(complaint.assignedTo()).contains(STAFF);
        assertThat(complaint.history().get(1))
                .isEqualTo(new StatusChangeSnapshot(2, ComplaintStatus.OPEN, ComplaintStatus.ASSIGNED, STAFF, at,
                        Complaint.ASSIGNED_AUTOMATICALLY));
        assertThat(complaint.pullDomainEvents()).containsExactly(new ComplaintAssigned(complaint.id(), STAFF, STAFF, at));
    }
}
