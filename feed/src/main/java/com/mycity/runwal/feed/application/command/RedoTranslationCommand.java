package com.mycity.runwal.feed.application.command;

/**
 * The society admin has a post's stored machine translation made again (e.g. after a bad one).
 *
 * @param language the web app's language code: en, hi or mr
 */
public record RedoTranslationCommand(long societyId, String slug, String language) {
}
