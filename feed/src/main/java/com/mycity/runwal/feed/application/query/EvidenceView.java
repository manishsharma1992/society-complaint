package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.domain.model.post.Evidence;
import java.time.Duration;

/** A photo of a post, with a short-lived link to view it (for the author and moderators). */
public record EvidenceView(String url, String contentType, int widthPx, int heightPx) {

    /** Long enough to review a post; a fresh link comes with every read. */
    static final Duration VIEW_LINK_VALID_FOR = Duration.ofMinutes(15);

    static EvidenceView of(Evidence evidence, EvidenceStorage storage) {
        return new EvidenceView(storage.viewUrl(evidence.storageKey(), VIEW_LINK_VALID_FOR).toString(),
                evidence.contentType(), evidence.widthPx(), evidence.heightPx());
    }
}
