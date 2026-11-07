package com.mycity.runwal.residency.application.service;

import com.mycity.runwal.residency.application.port.CurrentActor;
import com.mycity.runwal.residency.application.query.FlatView;
import com.mycity.runwal.residency.application.query.FlatsOfMemberQuery;
import com.mycity.runwal.residency.application.query.GetFlatQuery;
import com.mycity.runwal.residency.application.query.AddOnOnQuery;
import com.mycity.runwal.residency.application.query.GetSocietyQuery;
import com.mycity.runwal.residency.application.query.NearbySocietiesQuery;
import com.mycity.runwal.residency.application.query.ListFlatsQuery;
import com.mycity.runwal.residency.application.query.ListTowersQuery;
import com.mycity.runwal.residency.application.query.SocietyView;
import com.mycity.runwal.residency.application.query.TowerView;
import com.mycity.runwal.residency.domain.model.actor.Actor;
import com.mycity.runwal.residency.domain.model.flat.Flat;
import com.mycity.runwal.residency.domain.model.flat.FlatRepository;
import com.mycity.runwal.residency.domain.model.society.AddOn;
import com.mycity.runwal.residency.domain.model.society.SocietyRepository;
import com.mycity.runwal.residency.domain.model.tower.Tower;
import com.mycity.runwal.residency.domain.model.tower.TowerRepository;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * Society, towers and flats are visible to every logged-in user (people pick their flat when signing up); who
 * lives in a flat only to the admins of its tower (India DPDP Act: show personal data only to those who need it).
 */
@ApplicationService
public class ResidencyQueryService {

    private final SocietyRepository societies;
    private final TowerRepository towers;
    private final FlatRepository flats;
    private final CurrentActor currentActor;

    public ResidencyQueryService(SocietyRepository societies, TowerRepository towers, FlatRepository flats,
                                 CurrentActor currentActor) {
        this.societies = societies;
        this.towers = towers;
        this.flats = flats;
        this.currentActor = currentActor;
    }

    @Transactional(readOnly = true)
    public Optional<SocietyView> society(GetSocietyQuery query) {
        return societies.findById(new SocietyId(query.societyId())).map(SocietyView::of);
    }

    /** For other contexts, before they do anything for a society; false for an unknown society. */
    @Transactional(readOnly = true)
    public boolean addOnOn(AddOnOnQuery query) {
        AddOn addOn = AddOn.from(query.addOn());
        return societies.findById(new SocietyId(query.societyId())).map(s -> s.isOn(addOn)).orElse(false);
    }

    /** For the feed: neighbours (itself included) that use the add-on; empty for an unknown society. */
    @Transactional(readOnly = true)
    public List<SocietyView> nearby(NearbySocietiesQuery query) {
        AddOn addOn = AddOn.from(query.addOn());
        return societies.findById(new SocietyId(query.societyId()))
                .map(home -> societies.findAll().stream()
                        .filter(other -> other.isOn(addOn) && home.isNear(other, query.meters()))
                        .map(SocietyView::of)
                        .toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<TowerView> towers(ListTowersQuery query) {
        return towers.findBySociety(new SocietyId(query.societyId())).stream().map(TowerView::of).toList();
    }

    /** The flats of a tower, or empty when the society has no such tower. */
    @Transactional(readOnly = true)
    public Optional<List<FlatView>> flats(ListFlatsQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        return towers.findById(societyId, new TowerId(query.towerId())).map(tower ->
                flats.findByTower(societyId, tower.id()).stream().map(flat -> forCaller(FlatView.of(flat, tower), flat))
                        .toList());
    }

    @Transactional(readOnly = true)
    public Optional<FlatView> flat(GetFlatQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        return flats.findById(societyId, new FlatId(query.flatId())).map(flat -> forCaller(viewOf(flat), flat));
    }

    /** Which flats someone is linked to: only for that person themself or an admin. */
    @Transactional(readOnly = true)
    public List<FlatView> flatsOfMember(FlatsOfMemberQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        UserId member = new UserId(query.userId());
        Actor actor = currentActor.actor();
        if (!actor.is(member) && !(actor.active() && actor.isAdminIn(societyId))) {
            throw new ActionNotAllowedException("You can only see your own flats");
        }
        List<Flat> memberFlats = flats.findByMember(societyId, member);
        Map<TowerId, Tower> towersById = towers.findBySociety(societyId).stream()
                .collect(Collectors.toMap(Tower::id, Function.identity()));
        return memberFlats.stream().map(flat -> FlatView.of(flat, towersById.get(flat.towerId()))).toList();
    }

    private FlatView forCaller(FlatView view, Flat flat) {
        Actor actor = currentActor.actor();
        return actor.active() && actor.manages(flat.societyId(), flat.towerId()) ? view : view.withoutMembers();
    }

    private FlatView viewOf(Flat flat) {
        Tower tower = towers.findById(flat.societyId(), flat.towerId()).orElseThrow();
        return FlatView.of(flat, tower);
    }
}
