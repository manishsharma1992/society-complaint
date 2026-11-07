package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.query.MyPostsQuery;
import com.mycity.runwal.feed.application.query.PostView;
import com.mycity.runwal.feed.application.service.PostingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Logged-in residents submit posts and follow their own posts. */
@RestController
class PostController {

    private final PostingService postingService;

    PostController(PostingService postingService) {
        this.postingService = postingService;
    }

    @PostMapping("/societies/{societyId}/posts")
    ResponseEntity<PostResponse> submit(@PathVariable long societyId, @Valid @RequestBody SubmitPostRequest request) {
        PostView post = postingService.submit(request.toCommand(societyId));
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(post));
    }

    @GetMapping("/societies/{societyId}/my-posts")
    List<PostResponse> myPosts(@PathVariable long societyId,
                               @RequestParam(defaultValue = "0") @Min(0) int page,
                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return postingService.myPosts(new MyPostsQuery(societyId, page, size)).stream()
                .map(PostResponse::from)
                .toList();
    }
}
