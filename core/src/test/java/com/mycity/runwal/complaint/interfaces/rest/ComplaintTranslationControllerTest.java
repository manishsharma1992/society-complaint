package com.mycity.runwal.complaint.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.complaint.application.query.ComplaintTranslationView;
import com.mycity.runwal.complaint.application.query.TranslateComplaintQuery;
import com.mycity.runwal.complaint.application.service.ComplaintTranslationService;
import com.mycity.runwal.complaint.application.service.TranslationUnavailableException;
import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ComplaintTranslationController.class)
class ComplaintTranslationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintTranslationService translations;

    @Test
    void returnsTheComplaintTranslated() throws Exception {
        given(translations.translate(new TranslateComplaintQuery(1, 7, "mr"))).willReturn(Optional.of(
                new ComplaintTranslationView("mr", "पाणी गळती", "स्वयंपाकघरातील नळ गळतो",
                        List.of(new ComplaintTranslationView.TranslatedCommentView(1, "प्लंबर 4 वाजता")))));

        mockMvc.perform(get("/complaint/societies/1/complaints/7/translations/mr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("mr"))
                .andExpect(jsonPath("$.title").value("पाणी गळती"))
                .andExpect(jsonPath("$.description").value("स्वयंपाकघरातील नळ गळतो"))
                .andExpect(jsonPath("$.comments[0].sequence").value(1))
                .andExpect(jsonPath("$.comments[0].text").value("प्लंबर 4 वाजता"));
    }

    @Test
    void missingIs404UnknownLanguage422AndServiceDown503() throws Exception {
        given(translations.translate(new TranslateComplaintQuery(1, 8, "mr"))).willReturn(Optional.empty());
        given(translations.translate(new TranslateComplaintQuery(1, 7, "fr")))
                .willThrow(new InvalidComplaintException("Unknown language"));
        given(translations.translate(new TranslateComplaintQuery(1, 7, "hi")))
                .willThrow(new TranslationUnavailableException("down", null));

        mockMvc.perform(get("/complaint/societies/1/complaints/8/translations/mr")).andExpect(status().isNotFound());
        mockMvc.perform(get("/complaint/societies/1/complaints/7/translations/fr"))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(get("/complaint/societies/1/complaints/7/translations/hi"))
                .andExpect(status().isServiceUnavailable());
    }
}
