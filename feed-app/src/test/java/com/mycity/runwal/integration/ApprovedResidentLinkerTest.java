package com.mycity.runwal.integration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mycity.runwal.identity.domain.event.RegistrationApproved;
import com.mycity.runwal.identity.domain.model.user.FlatClaim;
import com.mycity.runwal.identity.domain.model.user.Occupancy;
import com.mycity.runwal.residency.application.command.AddFlatMemberCommand;
import com.mycity.runwal.residency.application.query.FlatView;
import com.mycity.runwal.residency.application.service.FlatMembershipService;
import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class ApprovedResidentLinkerTest {

    private static final RegistrationApproved APPROVED = new RegistrationApproved(new UserId(100),
            new FlatClaim(new SocietyId(1), new FlatId(667), Occupancy.TENANT), new UserId(1),
            Instant.parse("2026-10-25T08:00:00Z"));

    private final FlatMembershipService memberships = mock(FlatMembershipService.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final ApprovedResidentLinker linker = new ApprovedResidentLinker(memberships, transactions);

    @Test
    void approvedResidentIsLinkedToTheClaimedFlat() {
        given(transactions.getTransaction(any())).willReturn(new SimpleTransactionStatus());
        AddFlatMemberCommand expected = new AddFlatMemberCommand(1, 667, 100, "TENANT");
        given(memberships.addMember(expected))
                .willReturn(Optional.of(new FlatView(667, 7, "B2", 5, 503, "B2-503", List.of())));

        linker.on(APPROVED);

        verify(memberships).addMember(expected);
    }

    @Test
    void linkingProblemDoesNotUndoTheApproval() {
        given(transactions.getTransaction(any())).willReturn(new SimpleTransactionStatus());
        given(memberships.addMember(any())).willThrow(new InvalidResidencyException("already linked"));

        assertThatCode(() -> linker.on(APPROVED)).doesNotThrowAnyException();
    }
}
