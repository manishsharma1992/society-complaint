package com.mycity.runwal.residency.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.residency.application.command.OfferAddOnCommand;
import com.mycity.runwal.residency.application.command.SwitchAddOnCommand;
import com.mycity.runwal.residency.application.command.SwitchAddOnInTowerCommand;
import com.mycity.runwal.residency.application.query.AddOnOnInTowerQuery;
import com.mycity.runwal.residency.application.query.AddOnOnQuery;
import com.mycity.runwal.residency.application.query.AddOnView;
import com.mycity.runwal.residency.application.query.NearbySocietiesQuery;
import com.mycity.runwal.residency.application.query.SocietyView;
import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.residency.domain.model.actor.Actor;
import com.mycity.runwal.residency.domain.model.actor.ActorRole;
import com.mycity.runwal.residency.domain.model.society.AddOn;
import com.mycity.runwal.residency.domain.model.society.GeoPoint;
import com.mycity.runwal.residency.domain.model.society.Society;
import com.mycity.runwal.residency.domain.model.tower.Tower;
import com.mycity.runwal.residency.domain.model.tower.TowerCode;
import com.mycity.runwal.residency.domain.model.tower.TowerRepository;
import com.mycity.runwal.residency.domain.service.SocietySettingsPolicy;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SocietySettingsServiceTest {

    private static final SocietyId RUNWAL = new SocietyId(1);
    private static final SocietyId PALAVA = new SocietyId(2);
    private static final SocietyId THANE = new SocietyId(3);
    private static final Actor OWNER = new Actor(new UserId(1), true, Set.of(), true);
    private static final Actor SOCIETY_ADMIN = role(2, ActorRole.Part.SOCIETY_ADMIN, null);
    private static final Actor TOWER_ADMIN = role(3, ActorRole.Part.TOWER_ADMIN, 7L);
    private static final Actor RESIDENT = role(4, ActorRole.Part.RESIDENT, null);

    private final InMemorySocieties societies = new InMemorySocieties(
            new Society(RUNWAL, "Runwal MyCity", new GeoPoint(19.186062, 73.074812), Set.of(AddOn.FEED), Set.of()),
            new Society(PALAVA, "Palava", new GeoPoint(19.1580, 73.0300), Set.of(AddOn.FEED), Set.of(AddOn.FEED)),
            new Society(THANE, "Thane", new GeoPoint(19.2183, 72.9781), Set.of(AddOn.FEED), Set.of(AddOn.FEED)));
    private Actor caller = SOCIETY_ADMIN;
    private final SocietySettingsService settings =
            new SocietySettingsService(societies, new OneTower(), () -> caller, new SocietySettingsPolicy());
    private final ResidencyQueryService queries =
            new ResidencyQueryService(societies, null, null, () -> caller);

    private static Actor role(long id, ActorRole.Part part, Long towerId) {
        return new Actor(new UserId(id), true,
                Set.of(new ActorRole(part, RUNWAL, towerId == null ? null : new TowerId(towerId))));
    }

    private static AddOnView addOn(SocietyView view, String name) {
        return view.addOns().stream().filter(a -> a.addOn().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void thePlatformOwnerOffersAddOnsAndTheSocietyAdminSwitchesThemOn() {
        caller = OWNER;
        SocietyView offered = settings.offer(new OfferAddOnCommand(1, "COMPLAINTS", true)).orElseThrow();
        assertThat(addOn(offered, "COMPLAINTS")).isEqualTo(new AddOnView("COMPLAINTS", true, false));

        caller = SOCIETY_ADMIN;
        SocietyView on = settings.switchAddOn(new SwitchAddOnCommand(1, "COMPLAINTS", true)).orElseThrow();
        assertThat(addOn(on, "COMPLAINTS")).isEqualTo(new AddOnView("COMPLAINTS", true, true));
        assertThat(queries.addOnOn(new AddOnOnQuery(1, "COMPLAINTS"))).isTrue();

        caller = OWNER;
        settings.offer(new OfferAddOnCommand(1, "COMPLAINTS", false));
        assertThat(queries.addOnOn(new AddOnOnQuery(1, "COMPLAINTS"))).isFalse();
    }

    @Test
    void onlyThePlatformOwnerOffersAndOnlyItOrTheSocietyAdminSwitches() {
        for (Actor notOwner : List.of(SOCIETY_ADMIN, TOWER_ADMIN, RESIDENT)) {
            caller = notOwner;
            assertThatThrownBy(() -> settings.offer(new OfferAddOnCommand(1, "COMPLAINTS", true)))
                    .isInstanceOf(ActionNotAllowedException.class);
        }
        for (Actor notAdmin : List.of(TOWER_ADMIN, RESIDENT, new Actor(new UserId(2), false, SOCIETY_ADMIN.roles()))) {
            caller = notAdmin;
            assertThatThrownBy(() -> settings.switchAddOn(new SwitchAddOnCommand(1, "FEED", true)))
                    .isInstanceOf(ActionNotAllowedException.class);
        }
        caller = SOCIETY_ADMIN;
        assertThatThrownBy(() -> settings.switchAddOn(new SwitchAddOnCommand(2, "FEED", false)))
                .isInstanceOf(ActionNotAllowedException.class);
        caller = OWNER;
        assertThat(settings.switchAddOn(new SwitchAddOnCommand(2, "FEED", false))).isPresent();
    }

    @Test
    void anAddOnTheSocietyWasNotOfferedCannotBeSwitchedOnAndUnknownSocietiesAreEmpty() {
        assertThatThrownBy(() -> settings.switchAddOn(new SwitchAddOnCommand(1, "COMPLAINTS", true)))
                .isInstanceOf(InvalidResidencyException.class);
        assertThatThrownBy(() -> settings.switchAddOn(new SwitchAddOnCommand(1, "CHAT", true)))
                .isInstanceOf(InvalidResidencyException.class);
        caller = OWNER;
        assertThat(settings.offer(new OfferAddOnCommand(99, "FEED", true))).isEmpty();
        assertThat(queries.addOnOn(new AddOnOnQuery(99, "FEED"))).isFalse();
    }

    @Test
    void neighboursWithinTenKilometresThatUseTheFeedAreFound() {
        settings.switchAddOn(new SwitchAddOnCommand(1, "FEED", true));

        assertThat(queries.nearby(new NearbySocietiesQuery(1, 10_000, "FEED")))
                .extracting(SocietyView::name).containsExactly("Runwal MyCity", "Palava");

        settings.switchAddOn(new SwitchAddOnCommand(1, "FEED", false));
        assertThat(queries.nearby(new NearbySocietiesQuery(1, 10_000, "FEED")))
                .extracting(SocietyView::name).containsExactly("Palava");
        assertThat(queries.nearby(new NearbySocietiesQuery(99, 10_000, "FEED"))).isEmpty();
    }

    @Test
    void theSocietyAdminLeavesOutATowerWithItsOwnComplaintAppAndBringsItBack() {
        caller = OWNER;
        settings.offer(new OfferAddOnCommand(1, "COMPLAINTS", true));
        caller = SOCIETY_ADMIN;
        settings.switchAddOn(new SwitchAddOnCommand(1, "COMPLAINTS", true));

        SocietyView off = settings.switchAddOnInTower(new SwitchAddOnInTowerCommand(1, "COMPLAINTS", 5, false)).orElseThrow();
        assertThat(off.complaintsOffInTowers()).containsExactly(5L);
        assertThat(queries.addOnOnInTower(new AddOnOnInTowerQuery(1, "COMPLAINTS", 5))).isFalse();
        assertThat(queries.addOnOnInTower(new AddOnOnInTowerQuery(1, "COMPLAINTS", 6))).isTrue();
        assertThat(queries.addOnOn(new AddOnOnQuery(1, "COMPLAINTS"))).as("still on for the society").isTrue();

        settings.switchAddOnInTower(new SwitchAddOnInTowerCommand(1, "COMPLAINTS", 5, true));
        assertThat(queries.addOnOnInTower(new AddOnOnInTowerQuery(1, "COMPLAINTS", 5))).isTrue();
    }

    @Test
    void onlyTheSocietyAdminSwitchesTowersAndOnlyTheSocietysOwnTowers() {
        for (Actor notAdmin : List.of(TOWER_ADMIN, RESIDENT)) {
            caller = notAdmin;
            assertThatThrownBy(() -> settings.switchAddOnInTower(new SwitchAddOnInTowerCommand(1, "COMPLAINTS", 5, false)))
                    .isInstanceOf(ActionNotAllowedException.class);
        }
        caller = SOCIETY_ADMIN;
        assertThat(settings.switchAddOnInTower(new SwitchAddOnInTowerCommand(1, "COMPLAINTS", 99, false))).isEmpty();
        assertThatThrownBy(() -> settings.switchAddOnInTower(new SwitchAddOnInTowerCommand(1, "FEED", 5, false)))
                .isInstanceOf(InvalidResidencyException.class);
    }

    /** Runwal MyCity has tower 5 (B2); no other tower exists. */
    private static final class OneTower implements TowerRepository {

        private final Tower b2 = new Tower(new TowerId(5), RUNWAL, new TowerCode("B2"), "Radiant", 30);

        @Override
        public List<Tower> findBySociety(SocietyId societyId) {
            return societyId.equals(RUNWAL) ? List.of(b2) : List.of();
        }

        @Override
        public Optional<Tower> findById(SocietyId societyId, TowerId id) {
            return societyId.equals(RUNWAL) && id.equals(b2.id()) ? Optional.of(b2) : Optional.empty();
        }
    }
}
