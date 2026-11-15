package com.mycity.runwal.complaint.application.port;

/** Machine translation of text written in any language the service recognises. */
public interface Translator {

    /**
     * @param language the web app's language code: en, hi or mr
     * @return the text in that language; unchanged when it is already in it
     * @throws com.mycity.runwal.complaint.application.service.TranslationUnavailableException when the service
     *         cannot be used right now
     */
    String translate(String text, String language);
}
