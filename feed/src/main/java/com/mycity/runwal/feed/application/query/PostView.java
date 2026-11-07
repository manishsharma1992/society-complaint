package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.feed.application.port.EvidenceStorage;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.ShownTower;
import java.time.Instant;
import java.util.List;

/**
 * A post with its moderation state, for its author and for moderators. Never contains the author.
 *
 * @param reports open reports; only moderators see them (empty for the author)
 */
public record PostView(long id, String slug, String title, String body, String tower, String status,
                       Instant submittedAt, Instant publishedAt, List<EvidenceView> evidence,
                       List<ModerationView> moderation, List<ReportView> reports) {

    public static PostView forModerator(Post post, EvidenceStorage storage) {
        return of(post, storage, post.openReports().stream().map(ReportView::of).toList());
    }

    public static PostView forAuthor(Post post, EvidenceStorage storage) {
        return of(post, storage, List.of());
    }

    private static PostView of(Post post, EvidenceStorage storage, List<ReportView> reports) {
        return new PostView(post.id().value(), post.slug().value(), post.content().title(), post.content().body(),
                post.shownTower().map(ShownTower::label).orElse(null), post.status().name(), post.submittedAt(),
                post.publishedAt().orElse(null),
                post.evidence().stream().map(e -> EvidenceView.of(e, storage)).toList(), post.moderationLog().stream().map(ModerationView::of).toList(),
                reports);
    }
}
