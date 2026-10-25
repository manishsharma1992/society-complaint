package com.mycity.runwal.complaint.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.complaint.application.command.AddCommentCommand;
import com.mycity.runwal.complaint.application.query.CommentView;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.application.service.ComplaintCommentService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ComplaintCommentController.class)
class ComplaintCommentControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintCommentService commentService;

    @Test
    void addsCommentAndReturnsTheConversation() throws Exception {
        ComplaintView view = new ComplaintView(7, 1, "FLAT", 7L, 667L, 1, "Leak", null, "ASSIGNED", 4, 3L, NOW, NOW,
                List.of(), List.of(), List.of(new CommentView(1, 3, "Plumber at 4 pm", NOW)));
        given(commentService.comment(new AddCommentCommand(1, 7, "Plumber at 4 pm"))).willReturn(Optional.of(view));

        mockMvc.perform(post("/complaint/societies/1/complaints/7/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Plumber at 4 pm"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].authorId").value(3))
                .andExpect(jsonPath("$.comments[0].text").value("Plumber at 4 pm"))
                .andExpect(jsonPath("$.comments[0].postedAt").value("2026-10-25T08:00:00Z"));
    }

    @Test
    void emptyOrTooLongTextIs400() throws Exception {
        mockMvc.perform(post("/complaint/societies/1/complaints/7/comments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\": \" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/complaint/societies/1/complaints/7/comments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"" + "x".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(commentService);
    }

    @Test
    void unknownOrHiddenComplaintIs404() throws Exception {
        given(commentService.comment(new AddCommentCommand(1, 99, "Hi"))).willReturn(Optional.empty());

        mockMvc.perform(post("/complaint/societies/1/complaints/99/comments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"Hi\"}"))
                .andExpect(status().isNotFound());
    }
}
