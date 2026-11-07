package com.mycity.runwal.complaint.domain.model.complaint;

import com.mycity.runwal.shared.ddd.annotation.DomainRepository;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@DomainRepository
public interface ComplaintRepository {

    ComplaintId nextId();

    void save(Complaint complaint);

    Optional<Complaint> findById(SocietyId societyId, ComplaintId id);

    /**
     * Complaints of one society, newest first.
     *
     * @param status  only complaints in this status, or {@code null} for any status
     * @param towerId    only complaints in this tower, or {@code null} for any tower
     * @param assignedTo only complaints assigned to this staff member, or {@code null} for anyone's
     */
    List<Complaint> findBySociety(SocietyId societyId, ComplaintStatus status, TowerId towerId, UserId assignedTo,
                                  int page, int size);

    /**
     * Complaints raised in [from, to) that were assigned to a staff member, newest first (for the staff report).
     *
     * @param towerId only complaints in this tower, or {@code null} for any tower
     */
    List<Complaint> findAssignedRaisedBetween(SocietyId societyId, TowerId towerId, Instant from, Instant to,
                                              int page, int size);

    /**
     * What a resident may see, newest first: complaints they raised, complaints about their flats and all
     * common-area complaints of the society. Same optional filters as {@link #findBySociety}.
     */
    List<Complaint> findVisibleTo(SocietyId societyId, UserId resident, Set<FlatId> residentFlats,
                                  ComplaintStatus status, TowerId towerId, UserId assignedTo, int page, int size);
}
