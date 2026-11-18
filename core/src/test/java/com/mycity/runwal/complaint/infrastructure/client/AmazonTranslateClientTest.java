package com.mycity.runwal.complaint.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class AmazonTranslateClientTest {

    @Test
    void shortTextIsOneCall() {
        assertThat(AmazonTranslateClient.chunks("No gym\n\nBrochure p.4 promised a gym.", 9_000))
                .containsExactly("No gym\n\nBrochure p.4 promised a gym.");
    }

    @Test
    void longTextIsCutAtLineBreaksWithinTheByteLimit() {
        String line = "जिम नाही. ".repeat(20); // Devanagari: 3 bytes a letter
        String text = String.join("\n", line, line, line, line);

        List<String> chunks = AmazonTranslateClient.chunks(text, 1_000);

        assertThat(chunks).hasSizeGreaterThan(1)
                .allSatisfy(chunk -> assertThat(chunk.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(1_000));
        assertThat(String.join("\n", chunks)).isEqualTo(text);
    }

    @Test
    void aLineLongerThanTheLimitIsCutBetweenLetters() {
        String line = "😀".repeat(10); // 4 bytes each, two Java chars

        List<String> chunks = AmazonTranslateClient.chunks(line, 10);

        assertThat(chunks).containsExactly("😀😀", "😀😀", "😀😀", "😀😀", "😀😀");
    }
}
