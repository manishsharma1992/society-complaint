package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.command.ModeratePostCommand;
import com.mycity.runwal.feed.application.command.ModeratePostCommand.Decision;
import com.mycity.runwal.feed.application.query.ModerationQueueQuery;
import com.mycity.runwal.feed.application.service.ModerationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The moderation queue and decisions (society admins). Every decision is written to the moderation log. */
@RestController
@RequestMapping("/societies/{societyId}/moderation")
class ModerationController {

    private final ModerationService moderationService;

    ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /** Posts waiting for review (default), or e.g. ?status=HIDDEN for posts hidden by reports. */
    @GetMapping
    List<PostResponse> queue(@PathVariable long societyId,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") @Min(0) int page,
                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return moderationService.queue(new ModerationQueueQuery(societyId, status, page, size)).stream()
                .map(PostResponse::from)
                .toList();
    }

    @PostMapping("/{postId}/publish")
    ResponseEntity<PostResponse> publish(@PathVariable long societyId, @PathVariable long postId) {
        return decide(societyId, postId, Decision.PUBLISH, null);
    }

    @PostMapping("/{postId}/reject")
    ResponseEntity<PostResponse> reject(@PathVariable long societyId, @PathVariable long postId,
                                        @Valid @RequestBody ReasonRequest request) {
        return decide(societyId, postId, Decision.REJECT, request.reason());
    }

    @PostMapping("/{postId}/hide")
    ResponseEntity<PostResponse> hide(@PathVariable long societyId, @PathVariable long postId,
                                      @Valid @RequestBody ReasonRequest request) {
        return decide(societyId, postId, Decision.HIDE, request.reason());
    }

    @PostMapping("/{postId}/remove")
    ResponseEntity<PostResponse> remove(@PathVariable long societyId, @PathVariable long postId,
                                        @Valid @RequestBody ReasonRequest request) {
        return decide(societyId, postId, Decision.REMOVE, request.reason());
    }

    private ResponseEntity<PostResponse> decide(long societyId, long postId, Decision decision, String reason) {
        return ResponseEntity.of(moderationService.decide(new ModeratePostCommand(societyId, postId, decision, reason))
                .map(PostResponse::from));
    }
}
