package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.port.CurrentActor;
import com.mycity.runwal.complaint.application.query.StaffPerformanceQuery;
import com.mycity.runwal.complaint.application.query.StaffPerformanceView;
import com.mycity.runwal.complaint.domain.exception.InvalidComplaintException;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.complaint.Complaint;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintRepository;
import com.mycity.runwal.complaint.domain.model.complaint.ComplaintStatus;
import com.mycity.runwal.complaint.domain.service.ComplaintAccessPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.springframework.transaction.annotation.Transactional;

/**
 * The staff report: per maintenance staff member, how many complaints they were given, how fast they started and
 * resolved them, how many met the SLA, and what is still open. Admins see everyone (a tower admin only for their
 * tower); staff see only themselves.
 */
@ApplicationService
public class StaffReportService {

    static final Duration MAX_PERIOD = Duration.ofDays(400);
    private static final int PAGE_SIZE = 200;

    private final ComplaintRepository complaints;
    private final ComplaintAccessPolicy accessPolicy;
    private final CurrentActor currentActor;
    private final Clock clock;

    public StaffReportService(ComplaintRepository complaints, ComplaintAccessPolicy accessPolicy,
                              CurrentActor currentActor, Clock clock) {
        this.complaints = complaints;
        this.accessPolicy = accessPolicy;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StaffPerformanceView> report(StaffPerformanceQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        TowerId towerId = query.towerId() == null ? null : new TowerId(query.towerId());
        Actor actor = currentActor.actor();
        UserId onlyStaff;
        if (accessPolicy.maySeeStaffReport(actor, societyId, towerId)) {
            onlyStaff = null;
        } else if (accessPolicy.maySeeOwnStaffReport(actor, societyId)) {
            onlyStaff = actor.userId();
        } else {
            throw new ActionNotAllowedException(
                    "Only the society admin, the tower's admin or the staff member themself can see this report");
        }
        if (!query.to().isAfter(query.from())) {
            throw new InvalidComplaintException("The report period must end after it starts");
        }
        if (Duration.between(query.from(), query.to()).compareTo(MAX_PERIOD) > 0) {
            throw new InvalidComplaintException("The report period can be at most " + MAX_PERIOD.toDays() + " days");
        }
        Instant now = clock.instant();
        Map<Long, Row> rows = new TreeMap<>();
        forEachPage(page -> complaints.findAssignedRaisedBetween(societyId, towerId, query.from(), query.to(), page,
                PAGE_SIZE), complaint -> rowOf(rows, complaint, onlyStaff).ifPresent(row -> row.count(complaint)));
        for (ComplaintStatus active : List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS)) {
            forEachPage(page -> complaints.findBySociety(societyId, active, towerId, onlyStaff, page, PAGE_SIZE),
                    complaint -> rowOf(rows, complaint, onlyStaff).ifPresent(row -> row.backlog(complaint, now)));
        }
        return rows.entrySet().stream().map(entry -> entry.getValue().view(entry.getKey())).toList();
    }

    private static Optional<Row> rowOf(Map<Long, Row> rows, Complaint complaint, UserId onlyStaff) {
        return complaint.assignedTo()
                .filter(staff -> onlyStaff == null || onlyStaff.equals(staff))
                .map(staff -> rows.computeIfAbsent(staff.value(), id -> new Row()));
    }

    private static void forEachPage(PageReader reader, Consumer<Complaint> action) {
        for (int page = 0; ; page++) {
            List<Complaint> found = reader.read(page);
            found.forEach(action);
            if (found.size() < PAGE_SIZE) {
                return;
            }
        }
    }

    @FunctionalInterface
    private interface PageReader {
        List<Complaint> read(int page);
    }

    private static final class Row {
        private int assigned;
        private int started;
        private int resolved;
        private int rejected;
        private int resolvedWithinSla;
        private int openNow;
        private int overdueNow;
        private final List<Long> minutesToStart = new ArrayList<>();
        private final List<Long> minutesToResolve = new ArrayList<>();

        void count(Complaint complaint) {
            assigned++;
            Optional<Instant> assignedAt = complaint.reachedAt(ComplaintStatus.ASSIGNED);
            Optional<Instant> startedAt = complaint.reachedAt(ComplaintStatus.IN_PROGRESS);
            Optional<Instant> resolvedAt = complaint.reachedAt(ComplaintStatus.RESOLVED);
            if (startedAt.isPresent()) {
                started++;
                assignedAt.ifPresent(a -> minutesToStart.add(Duration.between(a, startedAt.get()).toMinutes()));
            }
            if (resolvedAt.isPresent()) {
                resolved++;
                assignedAt.ifPresent(a -> minutesToResolve.add(Duration.between(a, resolvedAt.get()).toMinutes()));
            }
            if (complaint.resolvedWithinSla()) {
                resolvedWithinSla++;
            }
            if (complaint.status() == ComplaintStatus.REJECTED) {
                rejected++;
            }
        }

        void backlog(Complaint complaint, Instant now) {
            openNow++;
            if (complaint.isOverdue(now)) {
                overdueNow++;
            }
        }

        StaffPerformanceView view(long staffId) {
            return new StaffPerformanceView(staffId, assigned, started, resolved, rejected, median(minutesToStart),
                    median(minutesToResolve), resolvedWithinSla, openNow, overdueNow);
        }

        private static Long median(List<Long> values) {
            if (values.isEmpty()) {
                return null;
            }
            List<Long> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
            int middle = sorted.size() / 2;
            return sorted.size() % 2 == 1 ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2;
        }
    }
}
