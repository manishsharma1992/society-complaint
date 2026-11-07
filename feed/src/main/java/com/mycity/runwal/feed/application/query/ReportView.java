package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.feed.domain.model.post.PostReport;
import java.time.Instant;

/** An open report, without who reported. */
public record ReportView(String reason, Instant reportedAt) {

    public static ReportView of(PostReport report) {
        return new ReportView(report.reason(), report.reportedAt());
    }
}
