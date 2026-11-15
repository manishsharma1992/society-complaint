package com.mycity.runwal.feed.application.query;

/** @param language the web app's language code: en, hi or mr */
public record TranslatePostQuery(String slug, String language) {
}
