package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.domain.model.post.Video;
import com.mycity.runwal.feed.domain.model.post.VideoStatus;
import java.util.List;

/**
 * A video of a post. Only a READY video has links, and they point to the server's clean copy and poster frame,
 * never to the original upload.
 *
 * @param status        PROCESSING, READY or FAILED
 * @param url           the playable MP4 (a 15-minute link); null unless READY
 * @param posterUrl     a frame to show before it plays (a 15-minute link); null unless READY
 * @param failureReason why it could not be used, for the author; null unless FAILED
 */
public record VideoView(String status, String url, String posterUrl, Integer widthPx, Integer heightPx,
                        Long durationMs, String failureReason) {

    static VideoView of(Video video, EvidenceStorage storage) {
        if (video.status() != VideoStatus.READY) {
            return new VideoView(video.status().name(), null, null, null, null, null, video.failureReason());
        }
        return new VideoView(video.status().name(),
                storage.viewUrl(video.playableKey(), EvidenceView.VIEW_LINK_VALID_FOR).toString(),
                storage.viewUrl(video.posterKey(), EvidenceView.VIEW_LINK_VALID_FOR).toString(),
                video.facts().widthPx(), video.facts().heightPx(), video.facts().durationMs(), null);
    }

    /** The videos everyone may watch: only READY ones. */
    static List<VideoView> watchable(List<Video> videos, EvidenceStorage storage) {
        return videos.stream().filter(v -> v.status() == VideoStatus.READY).map(v -> of(v, storage)).toList();
    }
}
