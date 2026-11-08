package com.mycity.runwal.complaint.infrastructure.messaging;

import com.mycity.runwal.complaint.application.service.AutoAssignmentService;
import com.mycity.runwal.complaint.domain.event.AutoAssignmentSwitched;
import com.mycity.runwal.complaint.domain.event.ComplaintRaised;
import com.mycity.runwal.complaint.domain.event.ComplaintRejected;
import com.mycity.runwal.complaint.domain.event.ComplaintResolved;
import com.mycity.runwal.complaint.domain.event.StaffSkillsChanged;
import com.mycity.runwal.shared.id.SocietyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs automatic assignment when something may have freed a staff member or added work: a complaint raised,
 * resolved or rejected, automatic assignment switched on, or a staff member's categories changed. Runs after the
 * change has committed; a failure here never undoes it. (A staff member starting a shift is wired in the app's
 * integration package, which may see both contexts.) Nothing polls, so the database can still sleep.
 */
@Component
class AutoAssignmentTrigger {

    private static final Logger log = LoggerFactory.getLogger(AutoAssignmentTrigger.class);

    private final AutoAssignmentService autoAssignment;

    AutoAssignmentTrigger(AutoAssignmentService autoAssignment) {
        this.autoAssignment = autoAssignment;
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(ComplaintRaised event) {
        run(event.societyId());
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(ComplaintResolved event) {
        run(event.societyId());
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(ComplaintRejected event) {
        run(event.societyId());
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(AutoAssignmentSwitched event) {
        if (event.on()) {
            run(event.societyId());
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(StaffSkillsChanged event) {
        run(event.societyId());
    }

    private void run(SocietyId societyId) {
        try {
            int assigned = autoAssignment.assignWaiting(societyId);
            if (assigned > 0) {
                log.info("Assigned {} waiting complaint(s) automatically in society {}", assigned, societyId.value());
            }
        } catch (RuntimeException e) {
            log.warn("Automatic assignment failed for society {}; complaints stay waiting", societyId.value(), e);
        }
    }
}
