package com.mycity.runwal.feed.infrastructure.messaging;

import com.mycity.runwal.feed.application.service.VideoProcessingService;
import com.mycity.runwal.feed.domain.event.VideoUploaded;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Processes videos on one background thread, one at a time, so the upload request returns at once and two
 * encodings never compete for the CPU. Starts a video once its upload has committed, and picks up videos left
 * waiting by a restart once the app is ready. Nothing polls, so the database can still sleep.
 */
@Component
class VideoProcessingTrigger {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingTrigger.class);

    private final VideoProcessingService videos;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "feed-video");
        thread.setDaemon(true);
        return thread;
    });

    VideoProcessingTrigger(VideoProcessingService videos) {
        this.videos = videos;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(VideoUploaded event) {
        worker.execute(() -> run(() -> videos.process(event.societyId(), event.postId(), event.storageKey())));
    }

    @EventListener(ApplicationReadyEvent.class)
    void pickUpWaitingVideos() {
        worker.execute(() -> run(() -> {
            int count = videos.processAllWaiting();
            if (count > 0) {
                log.info("Processed {} video(s) left waiting by a restart", count);
            }
        }));
    }

    private static void run(Runnable job) {
        try {
            job.run();
        } catch (RuntimeException e) {
            log.warn("Video processing stopped; waiting videos are picked up after the next restart", e);
        }
    }

    /** Interrupts a running encoding: its ffmpeg is killed and the video is processed again after the restart. */
    @PreDestroy
    void stop() {
        worker.shutdownNow();
    }
}
