package com.mycity.runwal.residency.application.service;

import com.mycity.runwal.residency.domain.model.society.Society;
import com.mycity.runwal.residency.domain.model.society.SocietyRepository;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemorySocieties implements SocietyRepository {

    final Map<SocietyId, Society> stored = new LinkedHashMap<>();

    InMemorySocieties(Society... societies) {
        for (Society society : societies) {
            stored.put(society.id(), society);
        }
    }

    @Override
    public Optional<Society> findById(SocietyId id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public List<Society> findAll() {
        return List.copyOf(stored.values());
    }

    @Override
    public void save(Society society) {
        stored.put(society.id(), society);
    }
}
