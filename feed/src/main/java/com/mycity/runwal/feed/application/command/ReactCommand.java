package com.mycity.runwal.feed.application.command;

/**
 * Someone, logged in or not, reacts to a published post with an emoji (one of {@code Emoji}'s names).
 *
 * @param reactorAddress the caller's network address; only a keyed hash of it is kept
 */
public record ReactCommand(String slug, String emoji, String reactorAddress) {
}
