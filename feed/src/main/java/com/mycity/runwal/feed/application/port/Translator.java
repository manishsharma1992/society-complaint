package com.mycity.runwal.feed.application.port;

import com.mycity.runwal.feed.domain.model.translation.Language;

/** Machine translation of text written in any language the service recognises. */
public interface Translator {

    /**
     * @return the text in {@code target}; unchanged when it is already in that language
     * @throws com.mycity.runwal.feed.application.service.TranslationUnavailableException when the service cannot
     *         be reached or refuses; trying again later may help
     */
    String translate(String text, Language target);
}
