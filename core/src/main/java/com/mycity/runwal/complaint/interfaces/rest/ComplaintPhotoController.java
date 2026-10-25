package com.mycity.runwal.complaint.interfaces.rest;

import com.mycity.runwal.complaint.application.command.AttachPhotoCommand;
import com.mycity.runwal.complaint.application.command.RequestPhotoUploadCommand;
import com.mycity.runwal.complaint.application.service.ComplaintPhotoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Photo upload in three calls: get an upload link here, PUT the file to it (straight to object storage),
 * then attach it here. Photos come back with view links in every complaint response.
 */
@RestController
@RequestMapping("/societies/{societyId}/complaints/{complaintId}/photos")
class ComplaintPhotoController {

    private final ComplaintPhotoService photoService;

    ComplaintPhotoController(ComplaintPhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping("/upload-url")
    ResponseEntity<PhotoUploadResponse> requestUpload(@PathVariable long societyId, @PathVariable long complaintId,
                                                      @Valid @RequestBody PhotoUploadRequest request) {
        return ResponseEntity.of(photoService.requestUpload(new RequestPhotoUploadCommand(
                        societyId, complaintId, request.contentType(), request.sizeBytes()))
                .map(PhotoUploadResponse::from));
    }

    @PostMapping
    ResponseEntity<ComplaintResponse> attach(@PathVariable long societyId, @PathVariable long complaintId,
                                             @Valid @RequestBody AttachPhotoRequest request) {
        return ResponseEntity.of(photoService.attach(
                        new AttachPhotoCommand(societyId, complaintId, request.storageKey()))
                .map(ComplaintResponse::from));
    }
}
