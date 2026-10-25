package com.mycity.runwal.complaint.interfaces.rest;

import com.mycity.runwal.complaint.application.command.AddCommentCommand;
import com.mycity.runwal.complaint.application.service.ComplaintCommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The conversation on a complaint; comments come back with the complaint. */
@RestController
@RequestMapping("/societies/{societyId}/complaints/{complaintId}/comments")
class ComplaintCommentController {

    private final ComplaintCommentService commentService;

    ComplaintCommentController(ComplaintCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    ResponseEntity<ComplaintResponse> comment(@PathVariable long societyId, @PathVariable long complaintId,
                                              @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.of(commentService.comment(new AddCommentCommand(societyId, complaintId, request.text()))
                .map(ComplaintResponse::from));
    }

    record CommentRequest(@NotBlank @Size(max = 1000) String text) {
    }
}
