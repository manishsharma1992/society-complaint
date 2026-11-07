package com.mycity.runwal.residency.application.service;

import com.mycity.runwal.residency.application.command.OfferAddOnCommand;
import com.mycity.runwal.residency.application.command.SwitchAddOnCommand;
import com.mycity.runwal.residency.application.port.CurrentActor;
import com.mycity.runwal.residency.application.query.SocietyView;
import com.mycity.runwal.residency.domain.model.actor.Actor;
import com.mycity.runwal.residency.domain.model.society.AddOn;
import com.mycity.runwal.residency.domain.model.society.SocietyRepository;
import com.mycity.runwal.residency.domain.service.SocietySettingsPolicy;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** A society's add-ons: offered by the platform owner, switched on or off by the society admin. */
@ApplicationService
public class SocietySettingsService {

    private final SocietyRepository societies;
    private final CurrentActor currentActor;
    private final SocietySettingsPolicy policy;

    public SocietySettingsService(SocietyRepository societies, CurrentActor currentActor, SocietySettingsPolicy policy) {
        this.societies = societies;
        this.currentActor = currentActor;
        this.policy = policy;
    }

    /** Empty when there is no such society. */
    @Transactional
    public Optional<SocietyView> offer(OfferAddOnCommand command) {
        Actor actor = currentActor.actor();
        if (!policy.mayOfferAddOns(actor)) {
            throw new ActionNotAllowedException("Only the platform owner decides which add-ons a society may use");
        }
        AddOn addOn = AddOn.from(command.addOn());
        return societies.findById(new SocietyId(command.societyId())).map(society -> {
            if (command.available()) {
                society.makeAvailable(addOn);
            } else {
                society.withdraw(addOn);
            }
            societies.save(society);
            return SocietyView.of(society);
        });
    }

    /** Empty when there is no such society. */
    @Transactional
    public Optional<SocietyView> switchAddOn(SwitchAddOnCommand command) {
        SocietyId societyId = new SocietyId(command.societyId());
        if (!policy.maySwitchAddOns(currentActor.actor(), societyId)) {
            throw new ActionNotAllowedException("Only the society admin can switch this society's add-ons");
        }
        AddOn addOn = AddOn.from(command.addOn());
        return societies.findById(societyId).map(society -> {
            if (command.on()) {
                society.switchOn(addOn);
            } else {
                society.switchOff(addOn);
            }
            societies.save(society);
            return SocietyView.of(society);
        });
    }
}
