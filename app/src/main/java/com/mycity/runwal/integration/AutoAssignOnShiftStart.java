package com.mycity.runwal.integration;

import com.mycity.runwal.complaint.application.service.AutoAssignmentService;
import com.mycity.runwal.workforce.domain.event.ShiftStarted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** A staff member starting a shift is free: Complaint Management may give them a waiting complaint. */
@Component
class AutoAssignOnShiftStart {

    private static final Logger log = LoggerFactory.getLogger(AutoAssignOnShiftStart.class);

    private final AutoAssignmentService autoAssignment;

    AutoAssignOnShiftStart(AutoAssignmentService autoAssignment) {
        this.autoAssignment = autoAssignment;
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(ShiftStarted event) {
        try {
            autoAssignment.assignWaiting(event.societyId());
        } catch (RuntimeException e) {
            log.warn("Automatic assignment after a shift start failed in society {}", event.societyId().value(), e);
        }
    }
}
