package com.mycity.runwal.feed.infrastructure.persistence;

import com.mycity.runwal.feed.domain.model.post.Evidence;
import com.mycity.runwal.feed.domain.model.post.ModerationSnapshot;
import com.mycity.runwal.feed.domain.model.post.Post;
import com.mycity.runwal.feed.domain.model.post.PostContent;
import com.mycity.runwal.feed.domain.model.post.PostId;
import com.mycity.runwal.feed.domain.model.post.PostReport;
import com.mycity.runwal.feed.domain.model.post.PostSlug;
import com.mycity.runwal.feed.domain.model.post.ShownTower;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;

final class PostMapper {

    private PostMapper() {
    }

    static PostJpaEntity toNewEntity(Post post) {
        PostJpaEntity entity = new PostJpaEntity(post.id().value());
        entity.societyId = post.societyId().value();
        entity.slug = post.slug().value();
        entity.authorId = post.author().value();
        entity.title = post.content().title();
        entity.body = post.content().body();
        entity.towerId = post.shownTower().map(t -> t.towerId().value()).orElse(null);
        entity.towerLabel = post.shownTower().map(ShownTower::label).orElse(null);
        entity.submittedAt = post.submittedAt();
        updateEntity(post, entity);
        return entity;
    }

    /**
     * Copies the parts of a post that change: status, first publication, the moderation log (only ever appended)
     * the open reports (only appended, or all cleared when the post is published again) and the photos (only
     * appended).
     */
    static void updateEntity(Post post, PostJpaEntity entity) {
        entity.status = post.status();
        entity.publishedAt = post.publishedAt().orElse(null);
        List<ModerationSnapshot> log = post.moderationLog();
        for (ModerationSnapshot entry : log.subList(entity.moderation.size(), log.size())) {
            entity.moderation.add(toEntity(entry));
        }
        List<PostReport> reports = post.openReports();
        if (reports.size() < entity.reports.size()) {
            entity.reports.clear();
        }
        for (PostReport report : reports.subList(entity.reports.size(), reports.size())) {
            entity.reports.add(toEntity(report));
        }
        List<Evidence> evidence = post.evidence();
        for (int i = entity.evidence.size(); i < evidence.size(); i++) {
            entity.evidence.add(toEntity(evidence.get(i), i + 1));
        }
    }

    static Post toDomain(PostJpaEntity entity) {
        return Post.reconstitute(
                new PostId(entity.id),
                new SocietyId(entity.societyId),
                new PostSlug(entity.slug),
                new UserId(entity.authorId),
                new PostContent(entity.title, entity.body),
                entity.towerId == null ? null : new ShownTower(new TowerId(entity.towerId), entity.towerLabel),
                entity.submittedAt,
                entity.status,
                entity.publishedAt,
                entity.moderation.stream().map(PostMapper::toSnapshot).toList(),
                entity.reports.stream().map(PostMapper::toReport).toList(),
                entity.evidence.stream().map(PostMapper::toEvidence).toList());
    }

    private static PostEvidenceJpaEntity toEntity(Evidence evidence, int position) {
        PostEvidenceJpaEntity entity = new PostEvidenceJpaEntity();
        entity.position = position;
        entity.storageKey = evidence.storageKey();
        entity.contentType = evidence.contentType();
        entity.sizeBytes = evidence.sizeBytes();
        entity.widthPx = evidence.widthPx();
        entity.heightPx = evidence.heightPx();
        entity.uploadedAt = evidence.uploadedAt();
        return entity;
    }

    private static Evidence toEvidence(PostEvidenceJpaEntity entity) {
        return new Evidence(entity.storageKey, entity.contentType, entity.sizeBytes, entity.widthPx, entity.heightPx,
                entity.uploadedAt);
    }

    private static PostModerationJpaEntity toEntity(ModerationSnapshot entry) {
        PostModerationJpaEntity entity = new PostModerationJpaEntity();
        entity.sequenceNo = entry.sequence();
        entity.fromStatus = entry.from();
        entity.toStatus = entry.to();
        entity.moderatorId = entry.moderator() == null ? null : entry.moderator().value();
        entity.moderatedAt = entry.at();
        entity.reason = entry.reason();
        return entity;
    }

    private static ModerationSnapshot toSnapshot(PostModerationJpaEntity entity) {
        return new ModerationSnapshot(entity.sequenceNo, entity.fromStatus, entity.toStatus,
                entity.moderatorId == null ? null : new UserId(entity.moderatorId), entity.moderatedAt, entity.reason);
    }

    private static PostReportJpaEntity toEntity(PostReport report) {
        PostReportJpaEntity entity = new PostReportJpaEntity();
        entity.reporterKey = report.reporterKey();
        entity.reason = report.reason();
        entity.reportedAt = report.reportedAt();
        return entity;
    }

    private static PostReport toReport(PostReportJpaEntity entity) {
        return new PostReport(entity.reporterKey, entity.reason, entity.reportedAt);
    }
}
