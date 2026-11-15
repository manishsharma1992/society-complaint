package com.mycity.runwal.feed.domain.model.translation;

import com.mycity.runwal.feed.domain.exception.InvalidPostException;
import java.util.Locale;

/** The languages the web app speaks, so the ones a post can be read in. */
public enum Language {
    EN, HI, MR;

    /** @param code the web app's language code: en, hi or mr */
    public static Language ofCode(String code) {
        for (Language language : values()) {
            if (language.code().equals(code)) {
                return language;
            }
        }
        throw new InvalidPostException("Unknown language: choose one of en, hi, mr");
    }

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }
}
