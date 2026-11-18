package com.mycity.runwal.complaint.infrastructure.client;

import com.mycity.runwal.complaint.application.port.Translator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Remembers recent translations, so pressing the button again or another reader of the same complaint is free. */
public class RememberingTranslatorAdapter implements Translator {

    private static final int REMEMBERED = 2_000;

    private final Translator translator;
    // shortcut: kept in memory (lost on restart, a few MB at most); store translations in the complaint schema if
    // translating gets expensive
    private final Map<String, String> recent = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > REMEMBERED;
        }
    });

    public RememberingTranslatorAdapter(Translator translator) {
        this.translator = translator;
    }

    @Override
    public String translate(String text, String language) {
        String key = language + ":" + text;
        String known = recent.get(key);
        if (known != null) {
            return known;
        }
        String translated = translator.translate(text, language);
        recent.put(key, translated);
        return translated;
    }
}
