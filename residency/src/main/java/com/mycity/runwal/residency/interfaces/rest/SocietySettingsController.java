package com.mycity.runwal.residency.interfaces.rest;

import com.mycity.runwal.residency.application.command.OfferAddOnCommand;
import com.mycity.runwal.residency.application.command.SwitchAddOnCommand;
import com.mycity.runwal.residency.application.command.SwitchAddOnInTowerCommand;
import com.mycity.runwal.residency.application.service.SocietySettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A society's add-ons (FEED, COMPLAINTS): the platform owner sets which are available, the society admin switches
 * available ones on or off, and may switch COMPLAINTS off for single towers. All answer with the whole society.
 */
@RestController
@RequestMapping("/societies/{societyId}/add-ons/{addOn}")
class SocietySettingsController {

    private final SocietySettingsService service;

    SocietySettingsController(SocietySettingsService service) {
        this.service = service;
    }

    @PutMapping("/availability")
    ResponseEntity<SocietyResponse> offer(@PathVariable long societyId, @PathVariable String addOn,
                                          @RequestBody AvailabilityRequest request) {
        return ResponseEntity.of(service.offer(new OfferAddOnCommand(societyId, addOn, request.available()))
                .map(SocietyResponse::from));
    }

    @PutMapping
    ResponseEntity<SocietyResponse> switchAddOn(@PathVariable long societyId, @PathVariable String addOn,
                                                @RequestBody SwitchRequest request) {
        return ResponseEntity.of(service.switchAddOn(new SwitchAddOnCommand(societyId, addOn, request.on()))
                .map(SocietyResponse::from));
    }

    /** A tower that handles complaints its own way ({@code on: false}), or that comes on board ({@code on: true}). */
    @PutMapping("/towers/{towerId}")
    ResponseEntity<SocietyResponse> switchInTower(@PathVariable long societyId, @PathVariable String addOn,
                                                  @PathVariable long towerId, @RequestBody SwitchRequest request) {
        return ResponseEntity.of(service.switchAddOnInTower(
                new SwitchAddOnInTowerCommand(societyId, addOn, towerId, request.on())).map(SocietyResponse::from));
    }

    record AvailabilityRequest(boolean available) {
    }

    record SwitchRequest(boolean on) {
    }
}
