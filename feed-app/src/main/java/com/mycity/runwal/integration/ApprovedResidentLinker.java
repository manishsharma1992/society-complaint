package com.mycity.runwal.integration;

import com.mycity.runwal.identity.domain.event.RegistrationApproved;
import com.mycity.runwal.residency.application.command.AddFlatMemberCommand;
import com.mycity.runwal.residency.application.service.ActionNotAllowedException;
import com.mycity.runwal.residency.application.service.FlatMembershipService;
import com.mycity.runwal.shared.ddd.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * When the society approves a self sign-up (Identity), links the resident to the claimed flat (Residency).
 * Runs after the approval has committed, in its own transaction: one aggregate per transaction, eventually
 * consistent. If linking fails (e.g. already linked), the approval stands and the admin links the flat by hand.
 */
@Component
class ApprovedResidentLinker {

    private static final Logger log = LoggerFactory.getLogger(ApprovedResidentLinker.class);

    private final FlatMembershipService memberships;
    private final TransactionTemplate newTransaction;

    ApprovedResidentLinker(FlatMembershipService memberships, PlatformTransactionManager transactionManager) {
        this.memberships = memberships;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(RegistrationApproved approved) {
        // Identity's OWNER/TENANT translated into Residency's membership types (same names today)
        // runs on the approving admin's request, so Residency sees the society admin as the one linking
        AddFlatMemberCommand link = new AddFlatMemberCommand(approved.claim().societyId().value(),
                approved.claim().flatId().value(), approved.userId().value(), approved.claim().occupancy().name());
        try {
            newTransaction.executeWithoutResult(status -> memberships.addMember(link)
                    .orElseThrow(() -> new IllegalStateException("Flat " + link.flatId() + " no longer exists")));
        } catch (DomainException | IllegalStateException | ActionNotAllowedException e) {
            log.warn("Approved user {} could not be linked to flat {}: {}", link.userId(), link.flatId(),
                    e.getMessage());
        }
    }
}
