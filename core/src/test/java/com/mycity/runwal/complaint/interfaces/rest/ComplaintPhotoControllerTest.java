package com.mycity.runwal.complaint.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.complaint.application.command.AttachPhotoCommand;
import com.mycity.runwal.complaint.application.command.RequestPhotoUploadCommand;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.application.query.PhotoUploadView;
import com.mycity.runwal.complaint.application.query.PhotoView;
import com.mycity.runwal.complaint.application.service.ComplaintPhotoService;
import com.mycity.runwal.shared.ddd.DomainException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ComplaintPhotoController.class)
class ComplaintPhotoControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");
    private static final String KEY = "complaints/1/7/u1.jpg";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintPhotoService photoService;

    @Test
    void handsOutUploadLink() throws Exception {
        given(photoService.requestUpload(new RequestPhotoUploadCommand(1, 7, "image/jpeg", 250_000)))
                .willReturn(Optional.of(new PhotoUploadView(KEY, "https://bucket/u1?sig", "image/jpeg", 250_000,
                        NOW.plusSeconds(600))));

        mockMvc.perform(post("/complaint/societies/1/complaints/7/photos/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType": "image/jpeg", "sizeBytes": 250000}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageKey").value(KEY))
                .andExpect(jsonPath("$.uploadUrl").value("https://bucket/u1?sig"))
                .andExpect(jsonPath("$.method").value("PUT"))
                .andExpect(jsonPath("$.headers.Content-Type").value("image/jpeg"))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-25T08:10:00Z"));
    }

    @Test
    void uploadLinkNeedsTypeAndSize() throws Exception {
        mockMvc.perform(post("/complaint/societies/1/complaints/7/photos/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType": "image/jpeg", "sizeBytes": 0}"""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(photoService);
    }

    @Test
    void unsupportedTypeIs422() throws Exception {
        given(photoService.requestUpload(any())).willThrow(new DomainException("A photo must be JPEG, PNG or WebP") { });

        mockMvc.perform(post("/complaint/societies/1/complaints/7/photos/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType": "image/gif", "sizeBytes": 1000}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("A photo must be JPEG, PNG or WebP"));
    }

    @Test
    void attachesPhotoAndReturnsComplaintWithViewLink() throws Exception {
        ComplaintView view = new ComplaintView(7, 1, "COMMON_AREA", 3L, null, 5, "Lift stuck", null, "OPEN", 10,
                null, NOW, NOW, List.of(),
                List.of(new PhotoView("https://bucket/u1?view", "image/jpeg", 250_000, 10, NOW)), List.of());
        given(photoService.attach(new AttachPhotoCommand(1, 7, KEY))).willReturn(Optional.of(view));

        mockMvc.perform(post("/complaint/societies/1/complaints/7/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storageKey": "complaints/1/7/u1.jpg"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos[0].url").value("https://bucket/u1?view"))
                .andExpect(jsonPath("$.photos[0].sizeBytes").value(250_000))
                .andExpect(jsonPath("$.photos[0].storageKey").doesNotExist());
    }

    @Test
    void unknownComplaintIs404() throws Exception {
        given(photoService.attach(any())).willReturn(Optional.empty());

        mockMvc.perform(post("/complaint/societies/1/complaints/99/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storageKey": "complaints/1/99/u1.jpg"}"""))
                .andExpect(status().isNotFound());
    }
}
