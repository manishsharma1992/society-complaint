package com.mycity.runwal.complaint.infrastructure.client;

import com.mycity.runwal.complaint.application.port.Translator;
import com.mycity.runwal.complaint.application.service.TranslationUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Google Cloud Translation (Basic, v2), used for Marathi: Amazon Translate dropped words from Marathi
 * translations. The source language is detected. The API key goes in a header, never in the URL (and so never in
 * a log line).
 */
// shortcut: one request per text; Google advises at most 5,000 characters, complaint texts are shorter, so no
// splitting. Split like AmazonTranslateClient if longer texts are ever allowed.
public class GoogleTranslateClient implements Translator {

    private static final URI ENDPOINT = URI.create("https://translation.googleapis.com/language/translate/v2");

    private final URI endpoint;
    private final String apiKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final JsonMapper json = JsonMapper.builder().build();

    public GoogleTranslateClient(String apiKey) {
        this(ENDPOINT, apiKey);
    }

    GoogleTranslateClient(URI endpoint, String apiKey) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    @Override
    public String translate(String text, String language) {
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("X-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            json.writeValueAsString(Map.of("q", text, "target", language, "format", "text"))))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw unavailable("Google Translate answered " + response.statusCode(), null);
            }
            String translated = json.readTree(response.body())
                    .path("data").path("translations").path(0).path("translatedText").asString();
            if (translated.isBlank()) {
                throw unavailable("Google Translate returned no text", null);
            }
            return translated;
        } catch (IOException | JacksonException failure) {
            throw unavailable("Google Translate could not be reached", failure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw unavailable("Interrupted while translating", interrupted);
        }
    }

    private static TranslationUnavailableException unavailable(String message, Throwable cause) {
        return new TranslationUnavailableException(message, cause);
    }
}
