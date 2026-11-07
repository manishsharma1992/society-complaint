package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;

/**
 * What a post says: a short title and the documented facts (what, where, when, and what was promised).
 * Written by the author and never translated.
 */
@ValueObject
public record PostContent(String title, String body) {

    public static final int MAX_TITLE_LENGTH = 150;
    public static final int MAX_BODY_LENGTH = 4000;

    public PostContent {
        if (title == null || title.isBlank()) {
            throw new InvalidPostException("A post needs a title");
        }
        if (body == null || body.isBlank()) {
            throw new InvalidPostException("A post needs a description of the facts");
        }
        title = title.strip();
        body = body.strip();
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new InvalidPostException("A post title can have at most " + MAX_TITLE_LENGTH + " characters");
        }
        if (body.length() > MAX_BODY_LENGTH) {
            throw new InvalidPostException("A post can have at most " + MAX_BODY_LENGTH + " characters");
        }
    }
}
