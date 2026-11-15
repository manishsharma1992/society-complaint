package com.mycity.runwal.feed.application.service;

import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.application.port.VideoTranscoder;
import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.Video;
import com.mycity.runwal.feed.domain.model.post.VideoFacts;
import com.mycity.runwal.feed.domain.model.post.VideoStatus;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns an uploaded video into the copy that is shown. Runs once per video, without a transaction (encoding takes
 * up to minutes); reads and the result go through {@link EvidenceService}, each in a short transaction of its own
 * (this runs on a background thread, where loading a post needs one). The
 * original is deleted once the result is recorded: it may carry the phone's location and is never needed again.
 * A video interrupted by a shutdown stays waiting and is processed again after the restart.
 */
@ApplicationService
public class VideoProcessingService {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);

    static final String COULD_NOT_PROCESS = "This video could not be processed. Please add it again.";

    private final EvidenceStorage storage;
    private final VideoTranscoder transcoder;
    private final EvidenceService evidence;

    public VideoProcessingService(EvidenceStorage storage, VideoTranscoder transcoder, EvidenceService evidence) {
        this.storage = storage;
        this.transcoder = transcoder;
        this.evidence = evidence;
    }

    /** Processes the video under this key if it still waits; does nothing otherwise. */
    public void process(SocietyId societyId, PostId postId, String storageKey) {
        Optional<Video> waiting = evidence.waitingVideo(societyId, postId, storageKey);
        if (waiting.isEmpty()) {
            return;
        }
        Video video = waiting.get();
        try {
            VideoFacts playable = storage.read(storageKey, in -> transcoder.transcode(in, (mp4, poster) -> {
                storage.put(video.playableKey(), mp4, "video/mp4");
                storage.put(video.posterKey(), poster, "image/jpeg");
            }));
            evidence.videoProcessed(societyId, postId, storageKey, playable);
        } catch (EvidenceRejectedException rejected) {
            fail(societyId, postId, video, rejected.getMessage());
            return;
        } catch (RuntimeException e) {
            if (Thread.currentThread().isInterrupted()) {
                throw e; // shutting down: the video stays waiting and is processed after the restart
            }
            log.warn("Video {} of post {} could not be processed", storageKey, postId.value(), e);
            fail(societyId, postId, video, COULD_NOT_PROCESS);
            return;
        }
        storage.delete(storageKey);
    }

    /** Processes every video still waiting, one after the other, e.g. after a restart; returns how many. */
    public int processAllWaiting() {
        int count = 0;
        for (Post post : evidence.postsWithVideoWaiting()) {
            for (Video video : post.videos()) {
                if (video.status() == VideoStatus.PROCESSING) {
                    process(post.societyId(), post.id(), video.storageKey());
                    count++;
                }
            }
        }
        return count;
    }

    private void fail(SocietyId societyId, PostId postId, Video video, String reason) {
        evidence.videoFailed(societyId, postId, video.storageKey(), reason);
        storage.delete(video.playableKey());
        storage.delete(video.posterKey());
        storage.delete(video.storageKey());
    }
}
