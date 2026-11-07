package com.mycity.runwal.feed.application.command;

/**
 * Anyone, logged in or not, reports a published post.
 *
 * @param reporterAddress the client's network address; only a keyed hash of it is kept, to count one report
 *                        per reporter
 */
public record ReportPostCommand(String slug, String reason, String reporterAddress) {
}
