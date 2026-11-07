package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintRepository;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintScope;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** In-memory {@link ComplaintRepository} for the service tests (filters and pages like the real one, unsorted). */
final class InMemoryComplaints implements ComplaintRepository {

    final Map<Long, Complaint> stored = new LinkedHashMap<>();
    int saves;
    private long sequence;

    @Override
    public ComplaintId nextId() {
        return new ComplaintId(++sequence);
    }

    @Override
    public void save(Complaint complaint) {
        saves++;
        stored.put(complaint.id().value(), complaint);
    }

    @Override
    public Optional<Complaint> findById(SocietyId societyId, ComplaintId id) {
        return Optional.ofNullable(stored.get(id.value())).filter(c -> c.societyId().equals(societyId));
    }

    @Override
    public List<Complaint> findBySociety(SocietyId societyId, ComplaintStatus status, TowerId towerId,
                                         UserId assignedTo, int page, int size) {
        return page(stored.values().stream().filter(c -> c.societyId().equals(societyId))
                .filter(c -> status == null || c.status() == status)
                .filter(c -> towerId == null || towerId.equals(c.location().towerId()))
                .filter(c -> assignedTo == null || c.assignedTo().filter(assignedTo::equals).isPresent())
                .toList(), page, size);
    }

    @Override
    public List<Complaint> findAssignedRaisedBetween(SocietyId societyId, TowerId towerId, Instant from, Instant to,
                                                     int page, int size) {
        return page(stored.values().stream().filter(c -> c.societyId().equals(societyId))
                .filter(c -> c.assignedTo().isPresent())
                .filter(c -> towerId == null || towerId.equals(c.location().towerId()))
                .filter(c -> !c.raisedAt().isBefore(from) && c.raisedAt().isBefore(to))
                .toList(), page, size);
    }

    private static List<Complaint> page(List<Complaint> all, int page, int size) {
        return all.stream().skip((long) page * size).limit(size).toList();
    }

    @Override
    public List<Complaint> findVisibleTo(SocietyId societyId, UserId resident, Set<FlatId> residentFlats,
                                         ComplaintStatus status, TowerId towerId, UserId assignedTo,
                                         int page, int size) {
        return findBySociety(societyId, status, towerId, assignedTo, page, size).stream()
                .filter(c -> c.raisedBy().equals(resident) || c.location().scope() == ComplaintScope.COMMON_AREA
                        || residentFlats.contains(c.location().flatId()))
                .toList();
    }
}
