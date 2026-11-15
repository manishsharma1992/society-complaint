package com.mycity.runwal.feed.infrastructure.client;

import com.mycity.runwal.feed.application.port.Translator;
import com.mycity.runwal.feed.application.service.TranslationUnavailableException;
import com.mycity.runwal.feed.domain.model.translation.Language;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.translate.TranslateClient;

/**
 * Amazon Translate. The source language is detected ("auto"), so posts written in Hindi or Marathi work too.
 * One call takes at most 10,000 bytes of UTF-8, and a long post in Devanagari (3 bytes a letter) can be more,
 * so longer texts are sent in pieces cut at line breaks.
 */
public class AmazonTranslateClient implements Translator, AutoCloseable {

    private static final int MAX_BYTES_PER_CALL = 9_000;

    private final TranslateClient client;

    public AmazonTranslateClient(TranslateClient client) {
        this.client = client;
    }

    @Override
    public String translate(String text, Language target) {
        try {
            return chunks(text, MAX_BYTES_PER_CALL).stream()
                    .map(chunk -> client.translateText(request -> request.text(chunk)
                            .sourceLanguageCode("auto")
                            .targetLanguageCode(target.code())).translatedText())
                    .collect(Collectors.joining("\n"));
        } catch (SdkException failure) {
            throw new TranslationUnavailableException("The translation service is not available right now", failure);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    /**
     * Whole lines, as many as fit in {@code maxBytes} (joined by line breaks); a line longer than that is cut,
     * never inside a letter. Blank lines where a piece starts are dropped.
     */
    static List<String> chunks(String text, int maxBytes) {
        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        text.lines().flatMap(line -> cut(line, maxBytes)).forEach(line -> {
            if (!chunk.isEmpty() && utf8Length(chunk + "\n" + line) > maxBytes) {
                chunks.add(chunk.toString());
                chunk.setLength(0);
            }
            if (!chunk.isEmpty()) {
                chunk.append('\n');
            }
            chunk.append(line);
        });
        if (!chunk.isEmpty()) {
            chunks.add(chunk.toString());
        }
        return chunks;
    }

    private static Stream<String> cut(String line, int maxBytes) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int bytes = 0;
        for (int i = 0; i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            int length = utf8Length(Character.toString(codePoint));
            if (bytes + length > maxBytes) {
                parts.add(line.substring(start, i));
                start = i;
                bytes = 0;
            }
            bytes += length;
            i += Character.charCount(codePoint);
        }
        parts.add(line.substring(start));
        return parts.stream();
    }

    private static int utf8Length(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
