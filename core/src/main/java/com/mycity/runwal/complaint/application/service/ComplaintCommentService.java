package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.command.AddCommentCommand;
import com.mycity.runwal.complaint.application.port.CurrentActor;
import com.mycity.runwal.complaint.application.port.DomainEventPublisher;
import com.mycity.runwal.complaint.application.port.FlatDirectory;
import com.mycity.runwal.complaint.application.port.PhotoStorage;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintRepository;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.time.Clock;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** The conversation on a complaint. Returns empty when the society has no such complaint, or the user may not see it. */
@ApplicationService
public class ComplaintCommentService {

    private final ComplaintRepository complaints;
    private final ComplaintAccessPolicy accessPolicy;
    private final CurrentActor currentActor;
    private final FlatDirectory flatDirectory;
    private final DomainEventPublisher eventPublisher;
    private final PhotoStorage photoStorage;
    private final Clock clock;

    public ComplaintCommentService(ComplaintRepository complaints, ComplaintAccessPolicy accessPolicy,
                                   CurrentActor currentActor, FlatDirectory flatDirectory,
                                   DomainEventPublisher eventPublisher, PhotoStorage photoStorage, Clock clock) {
        this.complaints = complaints;
        this.accessPolicy = accessPolicy;
        this.currentActor = currentActor;
        this.flatDirectory = flatDirectory;
        this.eventPublisher = eventPublisher;
        this.photoStorage = photoStorage;
        this.clock = clock;
    }

    @Transactional
    public Optional<ComplaintView> comment(AddCommentCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        Actor actor = currentActor.actor();
        return complaints.findById(societyId, new ComplaintId(command.complaintId()))
                .filter(complaint -> accessPolicy.mayComment(actor, complaint,
                        flatDirectory.flatsOf(societyId, actor.userId())))
                .map(complaint -> {
                    complaint.comment(actor.userId(), command.text(), clock.instant());
                    complaints.save(complaint);
                    eventPublisher.publish(complaint.pullDomainEvents());
                    return ComplaintView.of(complaint, photoStorage);
                });
    }
}
