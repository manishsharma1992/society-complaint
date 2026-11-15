package com.mycity.runwal.feed.application.query;

import com.mycity.runwal.feed.domain.model.translation.PostTranslation;

/** A published post's title and text machine-translated; like the public post, it has no author and no internal ID. */
public record PostTranslationView(String language, String title, String body) {

    public static PostTranslationView of(PostTranslation translation) {
        return new PostTranslationView(translation.language().code(), translation.title(), translation.body());
    }
}
