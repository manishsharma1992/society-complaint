package com.mycity.runwal.feed.interfaces.rest;

import com.mycity.runwal.feed.application.command.AttachEvidenceCommand;
import com.mycity.runwal.feed.application.command.RequestEvidenceUploadCommand;
import com.mycity.runwal.feed.application.service.EvidenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Photo upload in three calls: get an upload link here, PUT the file to it (straight to object storage),
 * then attach it here. Attaching checks the file and answers 422 with the reason when it is refused.
 */
@RestController
@RequestMapping("/societies/{societyId}/posts/{postId}/evidence")
class EvidenceController {

    private final EvidenceService evidenceService;

    EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping("/upload-url")
    ResponseEntity<EvidenceUploadResponse> requestUpload(@PathVariable long societyId, @PathVariable long postId,
                                                         @Valid @RequestBody EvidenceUploadRequest request) {
        return ResponseEntity.of(evidenceService.requestUpload(new RequestEvidenceUploadCommand(
                        societyId, postId, request.contentType(), request.sizeBytes()))
                .map(EvidenceUploadResponse::from));
    }

    @PostMapping
    ResponseEntity<PostResponse> attach(@PathVariable long societyId, @PathVariable long postId,
                                        @Valid @RequestBody AttachEvidenceRequest request) {
        return ResponseEntity.of(evidenceService.attach(new AttachEvidenceCommand(societyId, postId,
                        request.uploadKey()))
                .map(PostResponse::from));
    }
}
