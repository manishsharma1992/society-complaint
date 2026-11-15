package com.mycity.runwal.feed.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.feed.application.service.TranslationUnavailableException;
import com.mycity.runwal.feed.domain.model.translation.Language;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Against a local stand-in for Google's endpoint: what is sent, and what comes back. */
class GoogleTranslateClientTest {

    private final AtomicReference<String> sentBody = new AtomicReference<>();
    private final AtomicReference<String> sentKey = new AtomicReference<>();
    private HttpServer server;

    private GoogleTranslateClient answering(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/translate", exchange -> {
            sentBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sentKey.set(exchange.getRequestHeaders().getFirst("X-goog-api-key"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return new GoogleTranslateClient(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/translate"), "test-key");
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void sendsTheTextAndTargetWithTheKeyInAHeaderAndReadsTheTranslation() throws IOException {
        GoogleTranslateClient client = answering(200, """
                {"data": {"translations": [{"translatedText": "पाणी झिरपत आहे", "detectedSourceLanguage": "en"}]}}""");

        assertThat(client.translate("water is seeping", Language.MR)).isEqualTo("पाणी झिरपत आहे");
        assertThat(sentKey.get()).isEqualTo("test-key");
        assertThat(sentBody.get()).contains("\"q\":\"water is seeping\"", "\"target\":\"mr\"", "\"format\":\"text\"");
    }

    @Test
    void anErrorOrAnEmptyAnswerIsUnavailable() throws IOException {
        assertThatThrownBy(() -> answering(403, "{\"error\": {\"code\": 403}}").translate("x", Language.MR))
                .isInstanceOf(TranslationUnavailableException.class);
        stop();
        assertThatThrownBy(() -> answering(200, "{\"data\": {\"translations\": []}}").translate("x", Language.MR))
                .isInstanceOf(TranslationUnavailableException.class);
    }
}
