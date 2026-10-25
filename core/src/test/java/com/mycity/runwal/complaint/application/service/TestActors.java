package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.port.CurrentActor;
import com.mycity.runwal.complaint.application.port.FlatDirectory;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.actor.ActorRole;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Actors of society 1 for the service tests, and a {@link CurrentActor} the test can switch. */
final class TestActors {

    static final SocietyId SOCIETY = new SocietyId(1);

    private TestActors() {
    }

    static Actor resident(long userId) {
        return new Actor(new UserId(userId), true, Set.of(new ActorRole(ActorRole.Part.RESIDENT, SOCIETY, null)));
    }

    static Actor staff(long userId) {
        return new Actor(new UserId(userId), true, Set.of(new ActorRole(ActorRole.Part.STAFF, SOCIETY, null)));
    }

    static Actor towerAdmin(long userId, long towerId) {
        return new Actor(new UserId(userId), true,
                Set.of(new ActorRole(ActorRole.Part.TOWER_ADMIN, SOCIETY, new TowerId(towerId))));
    }

    static Actor societyAdmin(long userId) {
        return new Actor(new UserId(userId), true, Set.of(new ActorRole(ActorRole.Part.SOCIETY_ADMIN, SOCIETY, null)));
    }

    /** Whoever the test says is calling. */
    static final class Switchable implements CurrentActor {

        private Actor current;

        Switchable(Actor initial) {
            this.current = initial;
        }

        void become(Actor actor) {
            this.current = actor;
        }

        @Override
        public Actor actor() {
            return current;
        }
    }

    /** Flats by ID with their tower, and who lives where. */
    static FlatDirectory flats(Map<Long, Long> towerByFlat, Map<Long, Set<Long>> flatsByUser) {
        return new FlatDirectory() {
            @Override
            public Optional<TowerId> towerOf(SocietyId societyId, FlatId flatId) {
                return Optional.ofNullable(towerByFlat.get(flatId.value())).map(TowerId::new);
            }

            @Override
            public Set<FlatId> flatsOf(SocietyId societyId, UserId userId) {
                return flatsByUser.getOrDefault(userId.value(), Set.of()).stream()
                        .map(FlatId::new)
                        .collect(java.util.stream.Collectors.toSet());
            }
        };
    }
}
