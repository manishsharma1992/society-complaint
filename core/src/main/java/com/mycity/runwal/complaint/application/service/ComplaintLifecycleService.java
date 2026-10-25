package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.command.AssignComplaintCommand;
import com.mycity.runwal.complaint.application.command.CloseComplaintCommand;
import com.mycity.runwal.complaint.application.command.RejectComplaintCommand;
import com.mycity.runwal.complaint.application.command.ResolveComplaintCommand;
import com.mycity.runwal.complaint.application.command.StartWorkCommand;
import com.mycity.runwal.complaint.application.port.CurrentActor;
import com.mycity.runwal.complaint.application.port.DomainEventPublisher;
import com.mycity.runwal.complaint.application.port.PhotoStorage;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintRepository;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BiPredicate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves a complaint through its lifecycle, on behalf of the logged-in user.
 * Each method returns empty when the society has no such complaint.
 */
@ApplicationService
public class ComplaintLifecycleService {

    private final ComplaintRepository complaints;
    private final ComplaintAccessPolicy accessPolicy;
    private final CurrentActor currentActor;
    private final DomainEventPublisher eventPublisher;
    private final PhotoStorage photoStorage;
    private final Clock clock;

    public ComplaintLifecycleService(ComplaintRepository complaints, ComplaintAccessPolicy accessPolicy,
                                     CurrentActor currentActor, DomainEventPublisher eventPublisher,
                                     PhotoStorage photoStorage, Clock clock) {
        this.complaints = complaints;
        this.accessPolicy = accessPolicy;
        this.currentActor = currentActor;
        this.eventPublisher = eventPublisher;
        this.photoStorage = photoStorage;
        this.clock = clock;
    }

    @Transactional
    public Optional<ComplaintView> assign(AssignComplaintCommand command) {
        return change(command.societyId(), command.complaintId(), accessPolicy::mayAssignOrReject,
                "Only the society admin or this tower's admin can assign a complaint", (complaint, actor, now) ->
                        complaint.assignTo(new UserId(command.staffId()), actor.userId(), now));
    }

    @Transactional
    public Optional<ComplaintView> startWork(StartWorkCommand command) {
        return change(command.societyId(), command.complaintId(), accessPolicy::mayWorkOn,
                "Only the assigned staff member or an admin can start work", (complaint, actor, now) ->
                        complaint.startWork(actor.userId(), now));
    }

    @Transactional
    public Optional<ComplaintView> resolve(ResolveComplaintCommand command) {
        return change(command.societyId(), command.complaintId(), accessPolicy::mayWorkOn,
                "Only the assigned staff member or an admin can resolve a complaint", (complaint, actor, now) ->
                        complaint.resolve(actor.userId(), now, command.note()));
    }

    @Transactional
    public Optional<ComplaintView> close(CloseComplaintCommand command) {
        return change(command.societyId(), command.complaintId(), accessPolicy::mayClose,
                "Only the person who raised the complaint or an admin can close it", (complaint, actor, now) ->
                        complaint.close(actor.userId(), now));
    }

    @Transactional
    public Optional<ComplaintView> reject(RejectComplaintCommand command) {
        return change(command.societyId(), command.complaintId(), accessPolicy::mayAssignOrReject,
                "Only the society admin or this tower's admin can reject a complaint", (complaint, actor, now) ->
                        complaint.reject(actor.userId(), now, command.reason()));
    }

    private Optional<ComplaintView> change(long societyId, long complaintId, BiPredicate<Actor, Complaint> allowed,
                                           String notAllowed, Action action) {
        Actor actor = currentActor.actor();
        return complaints.findById(new SocietyId(societyId), new ComplaintId(complaintId)).map(complaint -> {
            if (!allowed.test(actor, complaint)) {
                throw new ActionNotAllowedException(notAllowed);
            }
            action.apply(complaint, actor, clock.instant());
            complaints.save(complaint);
            eventPublisher.publish(complaint.pullDomainEvents());
            return ComplaintView.of(complaint, photoStorage);
        });
    }

    @FunctionalInterface
    private interface Action {
        void apply(Complaint complaint, Actor actor, Instant now);
    }
}
