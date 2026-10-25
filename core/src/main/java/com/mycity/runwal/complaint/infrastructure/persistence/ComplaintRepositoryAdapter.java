package com.mycity.runwal.complaint.infrastructure.persistence;

import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintId;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintRepository;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintScope;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class ComplaintRepositoryAdapter implements ComplaintRepository {

    private final ComplaintJpaRepository jpaRepository;
    private final EntityManager entityManager;

    ComplaintRepositoryAdapter(ComplaintJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public ComplaintId nextId() {
        return new ComplaintId(jpaRepository.nextId());
    }

    @Override
    public void save(Complaint complaint) {
        jpaRepository.findById(complaint.id().value()).ifPresentOrElse(
                existing -> ComplaintMapper.updateEntity(complaint, existing),
                () -> entityManager.persist(ComplaintMapper.toNewEntity(complaint)));
    }

    @Override
    public Optional<Complaint> findById(SocietyId societyId, ComplaintId id) {
        return jpaRepository.findByIdAndSocietyId(id.value(), societyId.value()).map(ComplaintMapper::toDomain);
    }

    @Override
    public List<Complaint> findBySociety(SocietyId societyId, ComplaintStatus status, TowerId towerId,
                                         UserId assignedTo, int page, int size) {
        return jpaRepository.search(societyId.value(), status, towerId == null ? null : towerId.value(),
                        assignedTo == null ? null : assignedTo.value(), PageRequest.of(page, size))
                .stream()
                .map(ComplaintMapper::toDomain)
                .toList();
    }

    @Override
    public List<Complaint> findVisibleTo(SocietyId societyId, UserId resident, Set<FlatId> residentFlats,
                                         ComplaintStatus status, TowerId towerId, UserId assignedTo,
                                         int page, int size) {
        // "in ()" is not valid SQL: without flats, match a flat ID that cannot exist
        List<Long> flatIds = residentFlats.isEmpty() ? List.of(-1L) : residentFlats.stream().map(FlatId::value).toList();
        return jpaRepository.searchVisibleTo(societyId.value(), status, towerId == null ? null : towerId.value(),
                        assignedTo == null ? null : assignedTo.value(), resident.value(), ComplaintScope.COMMON_AREA, flatIds, PageRequest.of(page, size))
                .stream()
                .map(ComplaintMapper::toDomain)
                .toList();
    }
}
