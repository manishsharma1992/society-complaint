package com.mycity.runwal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.complaint.application.service.TranslationUnavailableException;
import com.mycity.runwal.feed.domain.model.translation.Language;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InProcessComplaintTranslatorTest {

    private final List<String> sent = new ArrayList<>();

    @Test
    void translatesThroughTheSharedClientAndRemembersRecentTexts() {
        InProcessComplaintTranslator translator = new InProcessComplaintTranslator((text, target) -> {
            sent.add(text);
            return target == Language.MR ? "नळ गळतो" : "नल टपक रहा है";
        });

        assertThat(translator.translate("Tap leaking", "mr")).isEqualTo("नळ गळतो");
        assertThat(translator.translate("Tap leaking", "mr")).isEqualTo("नळ गळतो");
        assertThat(translator.translate("Tap leaking", "hi")).isEqualTo("नल टपक रहा है");
        assertThat(sent).hasSize(2);
    }

    @Test
    void anUnavailableServiceIsReportedInTheComplaintContextsTerms() {
        InProcessComplaintTranslator translator = new InProcessComplaintTranslator((text, target) -> {
            throw new com.mycity.runwal.feed.application.service.TranslationUnavailableException("down", null);
        });

        assertThatThrownBy(() -> translator.translate("Tap leaking", "mr"))
                .isInstanceOf(TranslationUnavailableException.class);
    }
}
