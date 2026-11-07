package com.mycity.runwal.workforce.infrastructure.config;

import com.mycity.runwal.workforce.application.service.ShiftEvidenceRetentionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Every night, deletes the selfies and locations of shifts older than the retention period. */
@Configuration
@EnableScheduling
class ShiftEvidenceRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ShiftEvidenceRetentionJob.class);
    /** Stops a run that keeps finding work (it should not), so one night cannot loop forever. */
    private static final int MAX_BATCHES_PER_RUN = 100;

    private final ShiftEvidenceRetentionService retention;

    ShiftEvidenceRetentionJob(ShiftEvidenceRetentionService retention) {
        this.retention = retention;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Kolkata")
    void forgetExpiredEvidence() {
        int total = 0;
        for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
            int cleaned = retention.forgetExpiredBatch();
            total += cleaned;
            if (cleaned == 0) {
                break;
            }
        }
        if (total > 0) {
            log.info("Deleted selfies and locations of {} shifts past the retention period", total);
        }
    }
}
