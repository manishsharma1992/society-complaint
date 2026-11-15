package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.command.ReactCommand;
import com.mycity.runwal.feed.application.command.ReportPostCommand;
import com.mycity.runwal.feed.application.command.TakeBackReactionCommand;
import com.mycity.runwal.feed.application.query.GetPublicPostQuery;
import com.mycity.runwal.feed.application.query.ListPublicPostsQuery;
import com.mycity.runwal.feed.application.service.PublicFeedService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, no login: published posts, reacting to them and reporting them. Reads may be cached for a minute. */
@RestController
@RequestMapping("/public")
class PublicFeedController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

    private final PublicFeedService publicFeedService;

    PublicFeedController(PublicFeedService publicFeedService) {
        this.publicFeedService = publicFeedService;
    }

    @GetMapping("/societies/{societyId}/posts")
    ResponseEntity<List<PublicPostResponse>> list(@PathVariable long societyId,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        List<PublicPostResponse> posts = publicFeedService.list(new ListPublicPostsQuery(societyId, page, size))
                .stream()
                .map(PublicPostResponse::from)
                .toList();
        return ResponseEntity.ok().cacheControl(CACHE).body(posts);
    }

    @GetMapping("/posts/{slug}")
    ResponseEntity<PublicPostResponse> get(@PathVariable String slug) {
        return publicFeedService.get(new GetPublicPostQuery(slug))
                .map(post -> ResponseEntity.ok().cacheControl(CACHE).body(PublicPostResponse.from(post)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Anyone can report a published post; only a keyed hash of the caller's address is kept. */
    @PostMapping("/posts/{slug}/reports")
    ResponseEntity<Void> report(@PathVariable String slug, @Valid @RequestBody ReasonRequest request,
                                HttpServletRequest http) {
        boolean found = publicFeedService.report(new ReportPostCommand(slug, request.reason(), http.getRemoteAddr()));
        return found ? ResponseEntity.accepted().build() : ResponseEntity.notFound().build();
    }

    /** Anyone can react with one emoji per post; reacting again replaces it. Returns the post's counts. */
    @PutMapping("/posts/{slug}/reaction")
    ResponseEntity<ReactionsResponse> react(@PathVariable String slug, @Valid @RequestBody ReactionRequest request,
                                            HttpServletRequest http) {
        return ResponseEntity.of(publicFeedService.react(new ReactCommand(slug, request.emoji(), http.getRemoteAddr()))
                .map(ReactionsResponse::new));
    }

    @DeleteMapping("/posts/{slug}/reaction")
    ResponseEntity<ReactionsResponse> takeBack(@PathVariable String slug, HttpServletRequest http) {
        return ResponseEntity.of(publicFeedService.takeBackReaction(
                new TakeBackReactionCommand(slug, http.getRemoteAddr())).map(ReactionsResponse::new));
    }

    /** @param reactions emoji name -> how many reacted with it; emoji nobody chose are left out */
    record ReactionsResponse(Map<String, Integer> reactions) {
    }
}
