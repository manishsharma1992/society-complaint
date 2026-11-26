package com.mycity.runwal.residency.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.residency.application.command.AddFlatMemberCommand;
import com.mycity.runwal.residency.application.command.RemoveFlatMemberCommand;
import com.mycity.runwal.residency.application.query.FlatView;
import com.mycity.runwal.residency.application.query.FlatsOfMemberQuery;
import com.mycity.runwal.residency.application.query.GetFlatQuery;
import com.mycity.runwal.residency.application.query.GetSocietyQuery;
import com.mycity.runwal.residency.application.query.ListFlatsQuery;
import com.mycity.runwal.residency.application.query.ListTowersQuery;
import com.mycity.runwal.residency.domain.event.FlatMemberAdded;
import com.mycity.runwal.residency.domain.event.FlatMemberRemoved;
import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.residency.domain.model.actor.Actor;
import com.mycity.runwal.residency.domain.model.actor.ActorRole;
import com.mycity.runwal.residency.domain.model.flat.Flat;
import com.mycity.runwal.residency.domain.model.flat.FlatNumber;
import com.mycity.runwal.residency.domain.model.flat.FlatRepository;
import com.mycity.runwal.residency.domain.model.society.Society;
import com.mycity.runwal.residency.domain.model.society.SocietyRepository;
import com.mycity.runwal.residency.domain.model.tower.Tower;
import com.mycity.runwal.residency.domain.model.tower.TowerCode;
import com.mycity.runwal.residency.domain.model.tower.TowerRepository;
import com.mycity.runwal.shared.ddd.DomainEvent;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResidencyServicesTest {

    private static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");
    private static final SocietyId SOCIETY = new SocietyId(1);
    private static final Tower B2 = new Tower(new TowerId(7), SOCIETY, new TowerCode("B2"), "Radiant", 27);
    private static final Tower C4 = new Tower(new TowerId(8), SOCIETY, new TowerCode("C4"), "Vega", 16);

    private final Map<Long, Flat> flats = new LinkedHashMap<>();
    private final List<DomainEvent> published = new ArrayList<>();
    private final FlatRepository flatRepository = new InMemoryFlats();
    private final TowerRepository towerRepository = towers(B2, C4);
    private final SocietyRepository societyRepository = new InMemorySocieties(new Society(SOCIETY, "Runwal MyCity"));
    private Actor caller = actor(20, ActorRole.Part.TOWER_ADMIN, 7L);
    private final ResidencyQueryService queries =
            new ResidencyQueryService(societyRepository, towerRepository, flatRepository, () -> caller);
    private final FlatMembershipService memberships = new FlatMembershipService(flatRepository, towerRepository,
            () -> caller, published::addAll, Clock.fixed(NOW, ZoneOffset.UTC));

    private static Actor actor(long userId, ActorRole.Part part, Long towerId) {
        return new Actor(new UserId(userId), true,
                Set.of(new ActorRole(part, SOCIETY, towerId == null ? null : new TowerId(towerId))));
    }

    ResidencyServicesTest() {
        store(795, B2, 5, 3);
        store(796, B2, 5, 4);
        store(900, C4, 1, 1);
    }

    @Test
    void onlyAdminsOfTheTowerSeeWhoLivesInAFlat() {
        memberships.addMember(new AddFlatMemberCommand(1, 795, 10, "OWNER"));
        assertThat(queries.flat(new GetFlatQuery(1, 795))).hasValueSatisfying(f -> assertThat(f.members()).hasSize(1));

        caller = actor(30, ActorRole.Part.RESIDENT, null);
        assertThat(queries.flat(new GetFlatQuery(1, 795))).hasValueSatisfying(f -> assertThat(f.members()).isEmpty());
        assertThat(queries.flats(new ListFlatsQuery(1, 7))).hasValueSatisfying(list ->
                assertThat(list).allSatisfy(f -> assertThat(f.members()).isEmpty()));
        caller = actor(21, ActorRole.Part.TOWER_ADMIN, 8L);
        assertThat(queries.flat(new GetFlatQuery(1, 795))).hasValueSatisfying(f -> assertThat(f.members()).isEmpty());
        caller = actor(1, ActorRole.Part.SOCIETY_ADMIN, null);
        assertThat(queries.flat(new GetFlatQuery(1, 795))).hasValueSatisfying(f -> assertThat(f.members()).hasSize(1));
    }

    @Test
    void societyAndTowers() {
        assertThat(queries.society(new GetSocietyQuery(1))).hasValueSatisfying(s -> assertThat(s.name())
                .isEqualTo("Runwal MyCity"));
        assertThat(queries.society(new GetSocietyQuery(2))).isEmpty();
        assertThat(queries.towers(new ListTowersQuery(1))).extracting(t -> t.code()).containsExactly("B2", "C4");
    }

    @Test
    void flatsOfATowerWithLabels() {
        assertThat(queries.flats(new ListFlatsQuery(1, 7))).hasValueSatisfying(list ->
                assertThat(list).extracting(FlatView::label).containsExactly("B2-503", "B2-504"));
        assertThat(queries.flats(new ListFlatsQuery(1, 99))).isEmpty();
    }

    @Test
    void ownerOfTwoFlats() {
        memberships.addMember(new AddFlatMemberCommand(1, 795, 10, "OWNER"));
        FlatView flat504 = memberships.addMember(new AddFlatMemberCommand(1, 796, 10, "OWNER")).orElseThrow();

        assertThat(flat504.members()).singleElement().satisfies(m -> {
            assertThat(m.userId()).isEqualTo(10);
            assertThat(m.type()).isEqualTo("OWNER");
            assertThat(m.since()).isEqualTo(NOW);
        });
        assertThat(queries.flatsOfMember(new FlatsOfMemberQuery(1, 10)))
                .extracting(FlatView::label).containsExactly("B2-503", "B2-504");
        assertThat(published).hasSize(2).allMatch(e -> e instanceof FlatMemberAdded);
    }

    @Test
    void tenantMovesOut() {
        caller = actor(1, ActorRole.Part.SOCIETY_ADMIN, null);
        memberships.addMember(new AddFlatMemberCommand(1, 900, 11, "TENANT"));

        FlatView flat = memberships.removeMember(new RemoveFlatMemberCommand(1, 900, 11)).orElseThrow();

        assertThat(flat.label()).isEqualTo("C4-101");
        assertThat(flat.members()).isEmpty();
        assertThat(queries.flatsOfMember(new FlatsOfMemberQuery(1, 11))).isEmpty();
        assertThat(published.getLast()).isInstanceOf(FlatMemberRemoved.class);
    }

    @Test
    void onlyAdminsOfThatTowerChangeMembers() {
        caller = actor(21, ActorRole.Part.TOWER_ADMIN, 8L);
        assertThatThrownBy(() -> memberships.addMember(new AddFlatMemberCommand(1, 795, 10, "OWNER")))
                .isInstanceOf(ActionNotAllowedException.class);
        caller = actor(10, ActorRole.Part.RESIDENT, null);
        assertThatThrownBy(() -> memberships.addMember(new AddFlatMemberCommand(1, 795, 10, "OWNER")))
                .isInstanceOf(ActionNotAllowedException.class);

        caller = actor(1, ActorRole.Part.SOCIETY_ADMIN, null);
        assertThat(memberships.addMember(new AddFlatMemberCommand(1, 900, 10, "OWNER"))).isPresent();
        assertThat(flats.get(900L).hasMember(new UserId(10))).isTrue();
    }

    @Test
    void peopleSeeOnlyTheirOwnFlats() {
        memberships.addMember(new AddFlatMemberCommand(1, 795, 10, "OWNER"));

        caller = actor(10, ActorRole.Part.RESIDENT, null);
        assertThat(queries.flatsOfMember(new FlatsOfMemberQuery(1, 10))).hasSize(1);
        assertThatThrownBy(() -> queries.flatsOfMember(new FlatsOfMemberQuery(1, 11)))
                .isInstanceOf(ActionNotAllowedException.class);

        caller = new Actor(new UserId(100), false, Set.of());
        assertThat(queries.flatsOfMember(new FlatsOfMemberQuery(1, 100))).isEmpty();
    }

    @Test
    void flatsOfMemberShowsMembersOnlyToAdminsOfTheirTower() {
        memberships.addMember(new AddFlatMemberCommand(1, 795, 10, "OWNER"));

        caller = actor(21, ActorRole.Part.TOWER_ADMIN, 8L);
        assertThat(queries.flatsOfMember(new FlatsOfMemberQuery(1, 10))).singleElement()
                .satisfies(flat -> assertThat(flat.members()).isEmpty());

        caller = actor(20, ActorRole.Part.TOWER_ADMIN, 7L);
        assertThat(queries.flatsOfMember(new FlatsOfMemberQuery(1, 10))).singleElement()
                .satisfies(flat -> assertThat(flat.members()).hasSize(1));
    }

    @Test
    void unknownTypeOrFlat() {
        assertThatThrownBy(() -> memberships.addMember(new AddFlatMemberCommand(1, 795, 10, "FAMILY")))
                .isInstanceOf(InvalidResidencyException.class);
        assertThat(memberships.addMember(new AddFlatMemberCommand(2, 795, 10, "OWNER"))).isEmpty();
        assertThat(queries.flat(new GetFlatQuery(2, 795))).isEmpty();
        assertThat(published).isEmpty();
    }

    private void store(long id, Tower tower, int floor, int unit) {
        flats.put(id, new Flat(new FlatId(id), SOCIETY, tower.id(), new FlatNumber(floor, unit), List.of()));
    }

    private static TowerRepository towers(Tower... all) {
        return new TowerRepository() {
            @Override
            public List<Tower> findBySociety(SocietyId societyId) {
                return List.of(all).stream().filter(t -> t.societyId().equals(societyId)).toList();
            }

            @Override
            public Optional<Tower> findById(SocietyId societyId, TowerId id) {
                return findBySociety(societyId).stream().filter(t -> t.id().equals(id)).findFirst();
            }
        };
    }

    private final class InMemoryFlats implements FlatRepository {

        @Override
        public void save(Flat flat) {
            flats.put(flat.id().value(), flat);
        }

        @Override
        public Optional<Flat> findById(SocietyId societyId, FlatId id) {
            return Optional.ofNullable(flats.get(id.value())).filter(f -> f.societyId().equals(societyId));
        }

        @Override
        public List<Flat> findByTower(SocietyId societyId, TowerId towerId) {
            return flats.values().stream()
                    .filter(f -> f.societyId().equals(societyId) && f.towerId().equals(towerId))
                    .toList();
        }

        @Override
        public List<Flat> findByMember(SocietyId societyId, UserId userId) {
            return flats.values().stream()
                    .filter(f -> f.societyId().equals(societyId) && f.hasMember(userId))
                    .toList();
        }
    }
}
