package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.command.AttachPhotoCommand;
import com.mycity.runwal.complaint.application.command.RequestPhotoUploadCommand;
import com.mycity.runwal.complaint.application.port.CurrentActor;
import com.mycity.runwal.complaint.application.port.PhotoStorage;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.application.query.PhotoUploadView;
import com.mycity.runwal.complaint.domain.exception.PhotoNotUploadedException;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintPhoto;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintRepository;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Photos are uploaded straight to object storage, never through this service:
 * 1. {@link #requestUpload} checks the complaint can take the photo and hands out a short-lived upload link;
 * 2. the client PUTs the file to that link;
 * 3. {@link #attach} checks the file is really there and adds it to the complaint.
 * Both methods return empty when the society has no such complaint.
 */
@ApplicationService
public class ComplaintPhotoService {

    static final Duration UPLOAD_LINK_VALID_FOR = Duration.ofMinutes(10);

    private final ComplaintRepository complaints;
    private final ComplaintAccessPolicy accessPolicy;
    private final CurrentActor currentActor;
    private final PhotoStorage photoStorage;
    private final Clock clock;

    public ComplaintPhotoService(ComplaintRepository complaints, ComplaintAccessPolicy accessPolicy,
                                 CurrentActor currentActor, PhotoStorage photoStorage, Clock clock) {
        this.complaints = complaints;
        this.accessPolicy = accessPolicy;
        this.currentActor = currentActor;
        this.photoStorage = photoStorage;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<PhotoUploadView> requestUpload(RequestPhotoUploadCommand command) {
        return find(command.societyId(), command.complaintId()).map(complaint -> {
            String key = complaint.newPhotoKey(command.contentType(), command.sizeBytes(), UUID.randomUUID().toString());
            String url = photoStorage.uploadUrl(key, command.contentType(), command.sizeBytes(), UPLOAD_LINK_VALID_FOR)
                    .toString();
            return new PhotoUploadView(key, url, command.contentType(), command.sizeBytes(),
                    clock.instant().plus(UPLOAD_LINK_VALID_FOR));
        });
    }

    @Transactional
    public Optional<ComplaintView> attach(AttachPhotoCommand command) {
        return find(command.societyId(), command.complaintId()).map(complaint -> {
            String key = command.storageKey();
            String contentType = ComplaintPhoto.contentTypeOf(key);
            long size = photoStorage.sizeOf(key).orElseThrow(() -> new PhotoNotUploadedException(key));
            complaint.addPhoto(new ComplaintPhoto(key, contentType, size, currentActor.actor().userId(),
                    clock.instant()));
            complaints.save(complaint);
            return ComplaintView.of(complaint, photoStorage);
        });
    }

    /** The complaint, if it exists and the logged-in user may add photos to it. */
    private Optional<Complaint> find(long societyId, long complaintId) {
        return complaints.findById(new SocietyId(societyId), new ComplaintId(complaintId)).map(complaint -> {
            if (!accessPolicy.mayAddPhoto(currentActor.actor(), complaint)) {
                throw new ActionNotAllowedException(
                        "Only the person who raised the complaint, the assigned staff member or an admin can add photos");
            }
            return complaint;
        });
    }
}
