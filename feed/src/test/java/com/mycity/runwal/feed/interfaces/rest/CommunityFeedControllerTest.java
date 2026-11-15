package com.mycity.runwal.feed.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.feed.application.query.CommunityFeedQuery;
import com.mycity.runwal.feed.application.query.EvidenceView;
import com.mycity.runwal.feed.application.query.FeedPostQuery;
import com.mycity.runwal.feed.application.query.FeedPostView;
import com.mycity.runwal.feed.application.service.ActionNotAllowedException;
import com.mycity.runwal.feed.application.service.CommunityFeedService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommunityFeedController.class)
class CommunityFeedControllerTest {

    private static final FeedPostView POST = new FeedPostView("abcdefgh2345", "No gym", "Brochure p.4", "Palava",
            "B2 Radiant", Instant.parse("2026-11-06T06:00:00Z"),
            List.of(new EvidenceView("https://bucket.test/feed/2/7/a.jpg?get", "image/jpeg", 1600, 1200)),
            Map.of("THUMBS_UP", 4));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityFeedService service;

    @Test
    void listsAndOpensFeedPostsWithSocietyAndPhotosButNoAuthor() throws Exception {
        given(service.feed(new CommunityFeedQuery(1, 0, 20))).willReturn(List.of(POST));
        given(service.post(new FeedPostQuery(1, "abcdefgh2345"))).willReturn(Optional.of(POST));
        given(service.post(new FeedPostQuery(1, "zzzzzzzzzzzz"))).willReturn(Optional.empty());

        String json = mockMvc.perform(get("/feed/societies/1/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].society").value("Palava"))
                .andExpect(jsonPath("$[0].photos[0].widthPx").value(1600))
                .andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContainIgnoringCase("author");
        mockMvc.perform(get("/feed/societies/1/feed/abcdefgh2345")).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("No gym"));
        mockMvc.perform(get("/feed/societies/1/feed/zzzzzzzzzzzz")).andExpect(status().isNotFound());
    }

    @Test
    void outsidersAre403AndPageSizesAreBounded() throws Exception {
        given(service.feed(new CommunityFeedQuery(2, 0, 20))).willThrow(new ActionNotAllowedException("no"));

        mockMvc.perform(get("/feed/societies/2/feed")).andExpect(status().isForbidden());
        mockMvc.perform(get("/feed/societies/1/feed").param("size", "500")).andExpect(status().isBadRequest());
    }
}
