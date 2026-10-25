package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.port.CurrentActor;
import com.mycity.runwal.complaint.application.port.FlatDirectory;
import com.mycity.runwal.complaint.application.port.PhotoStorage;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.application.query.GetComplaintQuery;
import com.mycity.runwal.complaint.application.query.ListComplaintsQuery;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintRepository;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** Reads complaints, showing each user only what they may see (see {@link ComplaintAccessPolicy#maySee}). */
@ApplicationService
public class ComplaintQueryService {

    private final ComplaintRepository complaints;
    private final ComplaintAccessPolicy accessPolicy;
    private final CurrentActor currentActor;
    private final FlatDirectory flatDirectory;
    private final PhotoStorage photoStorage;

    public ComplaintQueryService(ComplaintRepository complaints, ComplaintAccessPolicy accessPolicy,
                                 CurrentActor currentActor, FlatDirectory flatDirectory, PhotoStorage photoStorage) {
        this.complaints = complaints;
        this.accessPolicy = accessPolicy;
        this.currentActor = currentActor;
        this.flatDirectory = flatDirectory;
        this.photoStorage = photoStorage;
    }

    @Transactional(readOnly = true)
    public List<ComplaintView> list(ListComplaintsQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        ComplaintStatus status = query.status() == null ? null : ComplaintStatus.from(query.status());
        TowerId towerId = query.towerId() == null ? null : new TowerId(query.towerId());
        UserId assignedTo = query.assignedTo() == null ? null : new UserId(query.assignedTo());
        Actor actor = currentActor.actor();
        List<Complaint> found;
        if (accessPolicy.maySeeAll(actor, societyId)) {
            found = complaints.findBySociety(societyId, status, towerId, assignedTo, query.page(), query.size());
        } else if (actor.active() && actor.belongsTo(societyId)) {
            found = complaints.findVisibleTo(societyId, actor.userId(), flatDirectory.flatsOf(societyId, actor.userId()),
                    status, towerId, assignedTo, query.page(), query.size());
        } else {
            found = List.of();
        }
        return found.stream().map(complaint -> ComplaintView.of(complaint, photoStorage)).toList();
    }

    /** Empty when there is no such complaint or the user may not see it (not revealing which). */
    @Transactional(readOnly = true)
    public Optional<ComplaintView> get(GetComplaintQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        Actor actor = currentActor.actor();
        return complaints.findById(societyId, new ComplaintId(query.complaintId()))
                .filter(complaint -> accessPolicy.maySee(actor, complaint,
                        flatDirectory.flatsOf(societyId, actor.userId())))
                .map(complaint -> ComplaintView.of(complaint, photoStorage));
    }
}
