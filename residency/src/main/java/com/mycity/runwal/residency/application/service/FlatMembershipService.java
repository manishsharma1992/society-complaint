package com.mycity.runwal.residency.application.service;

import com.mycity.runwal.residency.application.command.AddFlatMemberCommand;
import com.mycity.runwal.residency.application.command.RemoveFlatMemberCommand;
import com.mycity.runwal.residency.application.port.CurrentActor;
import com.mycity.runwal.residency.application.port.DomainEventPublisher;
import com.mycity.runwal.residency.application.query.FlatView;
import com.mycity.runwal.residency.domain.model.actor.Actor;
import com.mycity.runwal.residency.domain.model.flat.Flat;
import com.mycity.runwal.residency.domain.model.flat.FlatRepository;
import com.mycity.runwal.residency.domain.model.flat.MembershipType;
import com.mycity.runwal.residency.domain.model.tower.TowerRepository;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.FlatId;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * Links people to flats and unlinks them; only the society admin or the admin of the flat's tower may.
 * Each method returns empty when the society has no such flat.
 */
@ApplicationService
public class FlatMembershipService {

    private final FlatRepository flats;
    private final TowerRepository towers;
    private final CurrentActor currentActor;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public FlatMembershipService(FlatRepository flats, TowerRepository towers, CurrentActor currentActor,
                                 DomainEventPublisher eventPublisher, Clock clock) {
        this.flats = flats;
        this.towers = towers;
        this.currentActor = currentActor;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public Optional<FlatView> addMember(AddFlatMemberCommand command) {
        MembershipType type = MembershipType.from(command.type());
        return change(command.societyId(), command.flatId(), (flat, actor, now) ->
                flat.addMember(new UserId(command.userId()), type, actor.userId(), now));
    }

    @Transactional
    public Optional<FlatView> removeMember(RemoveFlatMemberCommand command) {
        return change(command.societyId(), command.flatId(), (flat, actor, now) ->
                flat.removeMember(new UserId(command.userId()), actor.userId(), now));
    }

    private Optional<FlatView> change(long societyId, long flatId, Action action) {
        Actor actor = currentActor.actor();
        return flats.findById(new SocietyId(societyId), new FlatId(flatId)).map(flat -> {
            if (!actor.active() || !actor.manages(flat.societyId(), flat.towerId())) {
                throw new ActionNotAllowedException(
                        "Only the society admin or this tower's admin can change who is linked to a flat");
            }
            action.apply(flat, actor, clock.instant());
            flats.save(flat);
            eventPublisher.publish(flat.pullDomainEvents());
            return FlatView.of(flat, towers.findById(flat.societyId(), flat.towerId()).orElseThrow());
        });
    }

    @FunctionalInterface
    private interface Action {
        void apply(Flat flat, Actor actor, Instant now);
    }
}
