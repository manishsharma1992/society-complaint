package com.mycity.runwal.feed.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.feed.application.command.AttachEvidenceCommand;
import com.mycity.runwal.feed.application.command.ModeratePostCommand;
import com.mycity.runwal.feed.application.command.ModeratePostCommand.Decision;
import com.mycity.runwal.feed.application.command.RedoTranslationCommand;
import com.mycity.runwal.feed.application.command.ReactCommand;
import com.mycity.runwal.feed.application.command.ReportPostCommand;
import com.mycity.runwal.feed.application.command.RequestEvidenceUploadCommand;
import com.mycity.runwal.feed.application.command.SubmitPostCommand;
import com.mycity.runwal.feed.application.command.TakeBackReactionCommand;
import com.mycity.runwal.feed.application.query.EvidenceUploadView;
import com.mycity.runwal.feed.application.query.EvidenceView;
import com.mycity.runwal.feed.application.query.GetPublicPostQuery;
import com.mycity.runwal.feed.application.query.ListPublicPostsQuery;
import com.mycity.runwal.feed.application.query.ModerationView;
import com.mycity.runwal.feed.application.query.PostTranslationView;
import com.mycity.runwal.feed.application.query.PostView;
import com.mycity.runwal.feed.application.query.PublicPostView;
import com.mycity.runwal.feed.application.query.ReportView;
import com.mycity.runwal.feed.application.query.TranslatePostQuery;
import com.mycity.runwal.feed.application.query.VideoView;
import com.mycity.runwal.feed.application.service.ActionNotAllowedException;
import com.mycity.runwal.feed.application.service.EvidenceService;
import com.mycity.runwal.feed.application.service.ModerationService;
import com.mycity.runwal.feed.application.service.PostTranslationService;
import com.mycity.runwal.feed.application.service.PostingService;
import com.mycity.runwal.feed.application.service.PublicFeedService;
import com.mycity.runwal.feed.application.service.TranslationUnavailableException;
import com.mycity.runwal.feed.domain.exception.EvidenceRejectedException;
import com.mycity.runwal.feed.domain.exception.PostingLimitReachedException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PostController.class, ModerationController.class, PublicFeedController.class,
        EvidenceController.class})
class FeedControllersTest {

    private static final Instant NOW = Instant.parse("2026-11-01T06:00:00Z");
    private static final PostView PENDING = new PostView(7, "abcdefgh2345", "No gym", "Facts", "B2 Radiant",
            "PENDING_REVIEW", NOW, null, List.of(new EvidenceView("https://bucket.test/feed/1/7/a.jpg?get", "image/jpeg",
            1600, 1200)), List.of(new VideoView("FAILED", null, null, null, null, null,
            "A video can be at most 60 seconds long")), List.of(new ModerationView("PENDING_REVIEW", NOW, null)),
            List.of(new ReportView("Names a guard", NOW)));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostingService postingService;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private PublicFeedService publicFeedService;

    @MockitoBean
    private PostTranslationService postTranslationService;

    @MockitoBean
    private EvidenceService evidenceService;

    @Test
    void residentSubmitsAPostUnderTheFeedPrefix() throws Exception {
        given(postingService.submit(new SubmitPostCommand(1, "No gym", "Facts", 7L))).willReturn(PENDING);

        String json = mockMvc.perform(post("/feed/societies/1/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "No gym", "body": "Facts", "showTowerId": 7}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.tower").value("B2 Radiant"))
                .andExpect(jsonPath("$.evidence[0].widthPx").value(1600))
                .andExpect(jsonPath("$.videos[0].status").value("FAILED"))
                .andExpect(jsonPath("$.videos[0].failureReason").value("A video can be at most 60 seconds long"))
                .andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContainIgnoringCase("author");
    }

    @Test
    void invalidPostsAndTheDailyLimitAreTheCallersProblem() throws Exception {
        mockMvc.perform(post("/feed/societies/1/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\", \"body\": \"x\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(postingService);

        given(postingService.submit(any())).willThrow(new PostingLimitReachedException(5));
        mockMvc.perform(post("/feed/societies/1/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"t\", \"body\": \"x\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void publicReadsAreCacheableAndCarryNoAuthorOrId() throws Exception {
        PublicPostView view = new PublicPostView("abcdefgh2345", "No gym", "Facts", null, NOW,
                List.of(new EvidenceView("https://bucket.test/feed/1/7/a.jpg?get", "image/jpeg", 1600, 1200)),
                List.of(new VideoView("READY", "https://bucket.test/feed/1/7/v-play.mp4?get",
                        "https://bucket.test/feed/1/7/v-poster.jpg?get", 720, 1280, 6000L, null)),
                Map.of("HEART", 2));
        given(publicFeedService.list(new ListPublicPostsQuery(1, 0, 20))).willReturn(List.of(view));
        given(publicFeedService.get(new GetPublicPostQuery("abcdefgh2345"))).willReturn(Optional.of(view));

        String list = mockMvc.perform(get("/feed/public/societies/1/posts"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=60, public"))
                .andExpect(jsonPath("$[0].slug").value("abcdefgh2345"))
                .andExpect(jsonPath("$[0].photos[0].widthPx").value(1600))
                .andExpect(jsonPath("$[0].videos[0].url").value("https://bucket.test/feed/1/7/v-play.mp4?get"))
                .andExpect(jsonPath("$[0].videos[0].posterUrl").value("https://bucket.test/feed/1/7/v-poster.jpg?get"))
                .andExpect(jsonPath("$[0].videos[0].durationMs").value(6000))
                .andExpect(jsonPath("$[0].videos[0].status").doesNotExist())
                .andExpect(jsonPath("$[0].reactions.HEART").value(2))
                .andReturn().getResponse().getContentAsString();
        assertThat(list).doesNotContainIgnoringCase("author").doesNotContain("\"id\"");
        mockMvc.perform(get("/feed/public/posts/abcdefgh2345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("No gym"));
        mockMvc.perform(get("/feed/public/posts/zzzzzzzzzzzz")).andExpect(status().isNotFound());
    }

    @Test
    void anyoneReadsAPublishedPostTranslated() throws Exception {
        given(postTranslationService.translate(new TranslatePostQuery("abcdefgh2345", "mr")))
                .willReturn(Optional.of(new PostTranslationView("mr", "जिम नाही", "माहितीपत्रक पान 4")));
        given(postTranslationService.translate(new TranslatePostQuery("abcdefgh2345", "hi")))
                .willThrow(new TranslationUnavailableException("down", null));

        mockMvc.perform(get("/feed/public/posts/abcdefgh2345/translations/mr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("mr"))
                .andExpect(jsonPath("$.title").value("जिम नाही"))
                .andExpect(jsonPath("$.body").value("माहितीपत्रक पान 4"));
        mockMvc.perform(get("/feed/public/posts/zzzzzzzzzzzz/translations/mr")).andExpect(status().isNotFound());
        mockMvc.perform(get("/feed/public/posts/abcdefgh2345/translations/hi"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void theSocietyAdminRedoesATranslation() throws Exception {
        given(postTranslationService.redo(new RedoTranslationCommand(1, "abcdefgh2345", "mr")))
                .willReturn(Optional.of(new PostTranslationView("mr", "पाणी झिरपत आहे", "बांधकामाचा दर्जा")));
        given(postTranslationService.redo(new RedoTranslationCommand(1, "abcdefgh2345", "hi")))
                .willThrow(new ActionNotAllowedException("Only the society admin can redo a translation"));

        mockMvc.perform(post("/feed/societies/1/moderation/posts/abcdefgh2345/translations/mr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("पाणी झिरपत आहे"));
        mockMvc.perform(post("/feed/societies/1/moderation/posts/zzzzzzzzzzzz/translations/mr"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/feed/societies/1/moderation/posts/abcdefgh2345/translations/hi"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anyoneReactsToAPublishedPostAndGetsTheNewCounts() throws Exception {
        given(publicFeedService.react(new ReactCommand("abcdefgh2345", "HEART", "127.0.0.1")))
                .willReturn(Optional.of(Map.of("HEART", 3)));
        given(publicFeedService.takeBackReaction(new TakeBackReactionCommand("abcdefgh2345", "127.0.0.1")))
                .willReturn(Optional.of(Map.of("HEART", 2)));

        mockMvc.perform(put("/feed/public/posts/abcdefgh2345/reaction").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\": \"HEART\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactions.HEART").value(3));
        mockMvc.perform(delete("/feed/public/posts/abcdefgh2345/reaction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactions.HEART").value(2));
        mockMvc.perform(put("/feed/public/posts/zzzzzzzzzzzz/reaction").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\": \"HEART\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/feed/public/posts/abcdefgh2345/reaction").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emoji\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anyoneReportsAPublishedPost() throws Exception {
        given(publicFeedService.report(new ReportPostCommand("abcdefgh2345", "Names a guard", "127.0.0.1")))
                .willReturn(true);

        mockMvc.perform(post("/feed/public/posts/abcdefgh2345/reports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Names a guard\"}"))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/feed/public/posts/zzzzzzzzzzzz/reports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void moderatorsDecideWithAReason() throws Exception {
        given(moderationService.decide(new ModeratePostCommand(1, 7, Decision.REJECT, "Names a guard")))
                .willReturn(Optional.of(PENDING));

        mockMvc.perform(post("/feed/societies/1/moderation/7/reject").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Names a guard\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reports[0].reason").value("Names a guard"));
        mockMvc.perform(post("/feed/societies/1/moderation/7/hide").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        given(moderationService.decide(new ModeratePostCommand(1, 7, Decision.PUBLISH, null)))
                .willThrow(new ActionNotAllowedException("Only the society admin can moderate the feed"));
        mockMvc.perform(post("/feed/societies/1/moderation/7/publish")).andExpect(status().isForbidden());
        verify(moderationService).decide(new ModeratePostCommand(1, 7, Decision.PUBLISH, null));
    }

    @Test
    void photoUploadLinkAndAttachWithReasonsForRefusal() throws Exception {
        given(evidenceService.requestUpload(new RequestEvidenceUploadCommand(1, 7, "image/jpeg", 300000)))
                .willReturn(Optional.of(new EvidenceUploadView("feed/1/7/upload/u.jpg", "https://bucket.test/put",
                        "image/jpeg", 300000, NOW)));
        mockMvc.perform(post("/feed/societies/1/posts/7/evidence/upload-url").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\": \"image/jpeg\", \"sizeBytes\": 300000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadKey").value("feed/1/7/upload/u.jpg"));

        given(evidenceService.attach(new AttachEvidenceCommand(1, 7, "feed/1/7/upload/u.jpg")))
                .willThrow(new EvidenceRejectedException("The photo still contains GPS location"));
        mockMvc.perform(post("/feed/societies/1/posts/7/evidence").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uploadKey\": \"feed/1/7/upload/u.jpg\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("The photo still contains GPS location"));
    }
}
