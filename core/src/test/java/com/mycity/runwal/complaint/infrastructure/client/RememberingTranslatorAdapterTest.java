package com.mycity.runwal.complaint.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.complaint.application.service.TranslationUnavailableException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RememberingTranslatorAdapterTest {

    private final List<String> sent = new ArrayList<>();

    @Test
    void translatesOncePerTextAndLanguage() {
        RememberingTranslatorAdapter translator = new RememberingTranslatorAdapter((text, language) -> {
            sent.add(text);
            return language.equals("mr") ? "नळ गळतो" : "नल टपक रहा है";
        });

        assertThat(translator.translate("Tap leaking", "mr")).isEqualTo("नळ गळतो");
        assertThat(translator.translate("Tap leaking", "mr")).isEqualTo("नळ गळतो");
        assertThat(translator.translate("Tap leaking", "hi")).isEqualTo("नल टपक रहा है");
        assertThat(sent).hasSize(2);
    }

    @Test
    void aFailureIsNotRemembered() {
        RememberingTranslatorAdapter translator = new RememberingTranslatorAdapter((text, language) -> {
            sent.add(text);
            if (sent.size() == 1) {
                throw new TranslationUnavailableException("down", null);
            }
            return "नळ गळतो";
        });

        assertThatThrownBy(() -> translator.translate("Tap leaking", "mr"))
                .isInstanceOf(TranslationUnavailableException.class);
        assertThat(translator.translate("Tap leaking", "mr")).isEqualTo("नळ गळतो");
    }
}
