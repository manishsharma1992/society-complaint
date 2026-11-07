package com.mycity.runwal.residency.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.residency.domain.model.flat.Flat;
import com.mycity.runwal.residency.domain.model.flat.FlatRepository;
import com.mycity.runwal.residency.domain.model.flat.MembershipSnapshot;
import com.mycity.runwal.residency.domain.model.flat.MembershipType;
import com.mycity.runwal.residency.domain.model.society.AddOn;
import com.mycity.runwal.residency.domain.model.society.Society;
import com.mycity.runwal.residency.domain.model.society.SocietyRepository;
import com.mycity.runwal.residency.domain.model.tower.Tower;
import com.mycity.runwal.residency.domain.model.tower.TowerRepository;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Runs the Flyway migrations (including the Runwal MyCity seed) and the repository adapters on PostgreSQL. Needs Docker. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({ResidencySchemaConfig.class, SocietyRepositoryAdapter.class, TowerRepositoryAdapter.class,
        FlatRepositoryAdapter.class})
class ResidencyPersistenceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private static final SocietyId RUNWAL = new SocietyId(1);
    private static final UserId OWNER = new UserId(10);
    private static final UserId ADMIN = new UserId(20);
    // PostgreSQL stores microseconds
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private SocietyRepository societies;

    @Autowired
    private TowerRepository towers;

    @Autowired
    private FlatRepository flats;

    @Autowired
    private EntityManager entityManager;

    @Test
    void seedHasTheTenTowersAndTheirFlats() {
        assertThat(societies.findById(RUNWAL)).hasValueSatisfying(s -> assertThat(s.name()).isEqualTo("Runwal MyCity"));
        List<Tower> all = towers.findBySociety(RUNWAL);
        assertThat(all).extracting(t -> t.code().value() + " " + t.name()).containsExactly(
                "A1 Astoria", "C1 Celestia", "A2 Aster", "C2 Cosmos", "A3 Orion",
                "C3 Sirius", "B2 Radiant", "C4 Vega", "B1 Alpha", "A4 Polaris");
        int total = all.stream().mapToInt(t -> flats.findByTower(RUNWAL, t.id()).size()).sum();
        assertThat(total).isEqualTo(1036);

        Tower vega = all.get(7);
        List<Flat> vegaFlats = flats.findByTower(RUNWAL, vega.id());
        assertThat(vegaFlats).hasSize(64);
        assertThat(vegaFlats.getLast().number().label(vega.code())).isEqualTo("C4-1604");
        Tower radiant = all.get(6);
        assertThat(flats.findByTower(RUNWAL, radiant.id()))
                .extracting(f -> f.number().label(radiant.code()))
                .startsWith("B2-101", "B2-102", "B2-103", "B2-104", "B2-201")
                .contains("B2-503", "B2-504")
                .endsWith("B2-2704");
        assertThat(towers.findBySociety(new SocietyId(2))).isEmpty();
        // demo resident (identity user 4) owns B2-503
        assertThat(flats.findByMember(RUNWAL, new UserId(4)))
                .extracting(f -> f.number().label(radiant.code())).containsExactly("B2-503");
    }

    @Test
    void runwalMyCityHasItsLocationAndBothAddOnsAndAddOnChangesAreStored() {
        Society runwal = societies.findById(new SocietyId(1)).orElseThrow();
        assertThat(runwal.location()).hasValueSatisfying(l -> assertThat(l.latitude()).isEqualTo(19.186062));
        assertThat(runwal.enabledAddOns()).containsExactlyInAnyOrder(AddOn.FEED, AddOn.COMPLAINTS);

        runwal.switchOff(AddOn.FEED);
        runwal.withdraw(AddOn.COMPLAINTS);
        societies.save(runwal);
        flushAndClear();

        Society loaded = societies.findById(new SocietyId(1)).orElseThrow();
        assertThat(loaded.availableAddOns()).containsExactly(AddOn.FEED);
        assertThat(loaded.enabledAddOns()).isEmpty();
        assertThat(societies.findAll()).extracting(Society::id).contains(new SocietyId(1));
    }

    @Test
    void membersAreStoredAddedAndRemoved() {
        Tower radiant = towers.findBySociety(RUNWAL).get(6);
        List<Flat> radiantFlats = flats.findByTower(RUNWAL, radiant.id());
        Flat flat2601 = radiantFlats.get(100);
        Flat flat2602 = radiantFlats.get(101);
        flat2601.addMember(OWNER, MembershipType.OWNER, ADMIN, NOW);
        flat2602.addMember(OWNER, MembershipType.OWNER, ADMIN, NOW);
        flats.save(flat2601);
        flats.save(flat2602);
        flushAndClear();

        assertThat(flats.findByMember(RUNWAL, OWNER)).extracting(f -> f.number().value()).containsExactly(2601, 2602);
        Flat reloaded = flats.findById(RUNWAL, flat2601.id()).orElseThrow();
        assertThat(reloaded.members()).containsExactly(new MembershipSnapshot(OWNER, MembershipType.OWNER, NOW));

        reloaded.addMember(new UserId(11), MembershipType.TENANT, ADMIN, NOW);
        reloaded.removeMember(OWNER, ADMIN, NOW);
        flats.save(reloaded);
        flushAndClear();

        assertThat(flats.findById(RUNWAL, flat2601.id()).orElseThrow().members())
                .extracting(MembershipSnapshot::userId).containsExactly(new UserId(11));
        assertThat(flats.findByMember(RUNWAL, OWNER)).extracting(f -> f.number().value()).containsExactly(2602);
        assertThat(flats.findById(new SocietyId(2), flat2601.id())).isEmpty();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
