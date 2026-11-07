package com.mycity.runwal.feed.application.command;

/**
 * The logged-in resident submits a post for review.
 *
 * @param showTowerId the author's tower to show on the post, or {@code null} to show no tower
 */
public record SubmitPostCommand(long societyId, String title, String body, Long showTowerId) {
}
