package com.mycity.runwal.integration;

import com.mycity.runwal.complaint.application.port.Translator;
import com.mycity.runwal.complaint.application.service.TranslationUnavailableException;
import com.mycity.runwal.feed.domain.model.translation.Language;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Complaints are translated by the same Amazon Translate client as feed posts (one set of keys, one adapter).
 * Remembers recent translations, so pressing the button again or another reader of the same complaint is free.
 */
@Component
class InProcessComplaintTranslator implements Translator {

    private static final int REMEMBERED = 2_000;

    private final com.mycity.runwal.feed.application.port.Translator feedTranslator;
    // shortcut: kept in memory (lost on restart, a few MB at most); store translations in the complaint schema if
    // translating gets expensive
    private final Map<String, String> recent = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > REMEMBERED;
        }
    });

    InProcessComplaintTranslator(com.mycity.runwal.feed.application.port.Translator feedTranslator) {
        this.feedTranslator = feedTranslator;
    }

    @Override
    public String translate(String text, String language) {
        String key = language + ":" + text;
        String known = recent.get(key);
        if (known != null) {
            return known;
        }
        try {
            String translated = feedTranslator.translate(text, Language.ofCode(language));
            recent.put(key, translated);
            return translated;
        } catch (com.mycity.runwal.feed.application.service.TranslationUnavailableException unavailable) {
            throw new TranslationUnavailableException(unavailable.getMessage(), unavailable);
        }
    }
}
