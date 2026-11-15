package com.mycity.runwal.feed.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.feed.application.port.FeedAddOn;
import com.mycity.runwal.feed.application.service.EvidenceService;
import com.mycity.runwal.feed.application.service.ModerationService;
import com.mycity.runwal.feed.application.service.PostTranslationService;
import com.mycity.runwal.feed.application.service.PostingService;
import com.mycity.runwal.feed.application.service.PublicFeedService;
import com.mycity.runwal.shared.id.SocietyId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Without the Feed add-on, a society's feed endpoints answer 403. */
@WebMvcTest({PostController.class, ModerationController.class, PublicFeedController.class, EvidenceController.class})
class AddOnInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedAddOn addOn;

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
    void offMeans403ForTheSocietysPostsModerationAndListing() throws Exception {
        given(addOn.isOnFor(new SocietyId(2))).willReturn(false);

        mockMvc.perform(get("/feed/societies/2/my-posts")).andExpect(status().isForbidden());
        mockMvc.perform(get("/feed/societies/2/moderation")).andExpect(status().isForbidden());
        mockMvc.perform(get("/feed/public/societies/2/posts")).andExpect(status().isForbidden());
        verifyNoInteractions(postingService, moderationService, publicFeedService);
    }

    @Test
    void onLetsTheRequestThrough() throws Exception {
        given(addOn.isOnFor(new SocietyId(1))).willReturn(true);

        mockMvc.perform(get("/feed/societies/1/my-posts")).andExpect(status().isOk());
    }
}
