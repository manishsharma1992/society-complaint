package com.mycity.runwal.feed.domain.model.post;

import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import com.mycity.runwal.shared.ddd.annotation.ValueObject;
import java.util.regex.Pattern;

/**
 * The stable, unguessable part of a post's public link {@code /p/{slug}}. Random, so it reveals nothing about
 * the post's ID, its author or how many posts exist.
 */
@ValueObject
public record PostSlug(String value) {

    public static final int LENGTH = 12;
    private static final Pattern FORMAT = Pattern.compile("[a-z0-9]{" + LENGTH + "}");

    public PostSlug {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidPostException("A post link is " + LENGTH + " lowercase letters or digits");
        }
    }
}
