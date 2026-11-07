package com.mycity.runwal.workforce.application.service;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import com.mycity.runwal.workforce.domain.model.shift.Shift;
import com.mycity.runwal.workforce.domain.model.shift.ShiftId;
import com.mycity.runwal.workforce.domain.model.shift.ShiftRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryShifts implements ShiftRepository {

    private final Map<ShiftId, Shift> shifts = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public ShiftId nextId() {
        return new ShiftId(nextId++);
    }

    @Override
    public void save(Shift shift) {
        shifts.put(shift.id(), shift);
    }

    @Override
    public Optional<Shift> findOpen(SocietyId societyId, UserId staffId) {
        return shifts.values().stream()
                .filter(s -> s.societyId().equals(societyId) && s.staffId().equals(staffId) && s.isOpen())
                .findFirst();
    }

    @Override
    public List<Shift> findAllOpen(SocietyId societyId) {
        return shifts.values().stream().filter(s -> s.societyId().equals(societyId) && s.isOpen()).toList();
    }

    @Override
    public List<Shift> findOverlapping(SocietyId societyId, Instant from, Instant to) {
        return shifts.values().stream()
                .filter(s -> s.societyId().equals(societyId) && s.startedAt().isBefore(to)
                        && s.endedAt().map(end -> end.isAfter(from)).orElse(true))
                .sorted(Comparator.comparing(Shift::startedAt))
                .toList();
    }
}
