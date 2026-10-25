package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.command.RaiseComplaintCommand;
import com.mycity.runwal.complaint.application.port.CurrentActor;
import com.mycity.runwal.complaint.application.port.DomainEventPublisher;
import com.mycity.runwal.complaint.application.port.FlatDirectory;
import com.mycity.runwal.complaint.application.port.PhotoStorage;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.domain.exception.ComplaintCategoryNotFoundException;
import com.mycity.runwal.complaint.domain.exception.FlatNotFoundException;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryRepository;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintLocation;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintRepository;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintScope;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintTitle;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.complaint.domain.service.SlaPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class RaiseComplaintService {

    private final ComplaintRepository complaints;
    private final ComplaintCategoryRepository categories;
    private final FlatDirectory flatDirectory;
    private final SlaPolicy slaPolicy;
    private final ComplaintAccessPolicy accessPolicy;
    private final CurrentActor currentActor;
    private final DomainEventPublisher eventPublisher;
    private final PhotoStorage photoStorage;
    private final Clock clock;

    public RaiseComplaintService(ComplaintRepository complaints, ComplaintCategoryRepository categories,
                                 FlatDirectory flatDirectory, SlaPolicy slaPolicy, ComplaintAccessPolicy accessPolicy,
                                 CurrentActor currentActor, DomainEventPublisher eventPublisher,
                                 PhotoStorage photoStorage, Clock clock) {
        this.complaints = complaints;
        this.categories = categories;
        this.flatDirectory = flatDirectory;
        this.slaPolicy = slaPolicy;
        this.accessPolicy = accessPolicy;
        this.currentActor = currentActor;
        this.eventPublisher = eventPublisher;
        this.photoStorage = photoStorage;
        this.clock = clock;
    }

    @Transactional
    public ComplaintView raise(RaiseComplaintCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        ComplaintCategoryId categoryId = new ComplaintCategoryId(command.categoryId());
        ComplaintCategory category = categories.findById(societyId, categoryId)
                .orElseThrow(() -> new ComplaintCategoryNotFoundException(societyId, categoryId));

        ComplaintLocation location = locationOf(societyId, command);
        Actor actor = currentActor.actor();
        if (!accessPolicy.mayRaise(actor, societyId, location, flatDirectory.flatsOf(societyId, actor.userId()))) {
            throw new ActionNotAllowedException("You can raise complaints only for your own flat or a common area");
        }

        Complaint complaint = Complaint.raise(complaints.nextId(), societyId, location,
                category, new ComplaintTitle(command.title()), command.description(),
                actor.userId(), clock.instant(), slaPolicy);

        complaints.save(complaint);
        eventPublisher.publish(complaint.pullDomainEvents());
        return ComplaintView.of(complaint, photoStorage);
    }

    private ComplaintLocation locationOf(SocietyId societyId, RaiseComplaintCommand command) {
        TowerId towerId = command.towerId() == null ? null : new TowerId(command.towerId());
        FlatId flatId = command.flatId() == null ? null : new FlatId(command.flatId());
        if (ComplaintScope.from(command.scope()) == ComplaintScope.COMMON_AREA) {
            return new ComplaintLocation(ComplaintScope.COMMON_AREA, towerId, flatId);
        }
        TowerId towerOfFlat = flatId == null ? null : flatDirectory.towerOf(societyId, flatId)
                .orElseThrow(() -> new FlatNotFoundException(societyId, flatId));
        return ComplaintLocation.flat(towerOfFlat, flatId);
    }
}
