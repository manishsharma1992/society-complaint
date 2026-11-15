package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.query.CommunityFeedQuery;
import com.mycity.runwal.feed.application.query.EvidenceView;
import com.mycity.runwal.feed.application.query.FeedPostQuery;
import com.mycity.runwal.feed.application.query.FeedPostView;
import com.mycity.runwal.feed.application.service.CommunityFeedService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in feed: published posts from the viewer's society and nearby ones. Never shows who wrote them. */
@RestController
@RequestMapping("/societies/{societyId}/feed")
class CommunityFeedController {

    private final CommunityFeedService service;

    CommunityFeedController(CommunityFeedService service) {
        this.service = service;
    }

    @GetMapping
    List<FeedPostResponse> feed(@PathVariable long societyId,
                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.feed(new CommunityFeedQuery(societyId, page, size)).stream().map(FeedPostResponse::of).toList();
    }

    @GetMapping("/{slug}")
    ResponseEntity<FeedPostResponse> post(@PathVariable long societyId, @PathVariable String slug) {
        return ResponseEntity.of(service.post(new FeedPostQuery(societyId, slug)).map(FeedPostResponse::of));
    }

    record FeedPostResponse(String slug, String title, String body, String society, String tower,
                            Instant publishedAt, List<PhotoResponse> photos, Map<String, Integer> reactions) {
        static FeedPostResponse of(FeedPostView view) {
            return new FeedPostResponse(view.slug(), view.title(), view.body(), view.society(), view.tower(),
                    view.publishedAt(), view.evidence().stream().map(PhotoResponse::of).toList(), view.reactions());
        }
    }

    record PhotoResponse(String url, int widthPx, int heightPx) {
        static PhotoResponse of(EvidenceView view) {
            return new PhotoResponse(view.url(), view.widthPx(), view.heightPx());
        }
    }
}
