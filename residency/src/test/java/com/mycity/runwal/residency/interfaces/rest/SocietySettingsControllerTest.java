package com.mycity.runwal.residency.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.residency.application.command.OfferAddOnCommand;
import com.mycity.runwal.residency.application.command.SwitchAddOnCommand;
import com.mycity.runwal.residency.application.command.SwitchAddOnInTowerCommand;
import com.mycity.runwal.residency.application.query.AddOnView;
import com.mycity.runwal.residency.application.query.SocietyView;
import com.mycity.runwal.residency.application.service.ActionNotAllowedException;
import com.mycity.runwal.residency.application.service.SocietySettingsService;
import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SocietySettingsController.class)
class SocietySettingsControllerTest {

    private static final SocietyView RUNWAL = new SocietyView(1, "Runwal MyCity", 19.186062, 73.074812,
            List.of(new AddOnView("FEED", true, true), new AddOnView("COMPLAINTS", true, false)), List.of(5L));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SocietySettingsService service;

    @Test
    void theOwnerOffersAndTheAdminSwitchesUnderTheResidencyPrefix() throws Exception {
        given(service.offer(new OfferAddOnCommand(1, "COMPLAINTS", true))).willReturn(Optional.of(RUNWAL));
        given(service.switchAddOn(new SwitchAddOnCommand(1, "FEED", true))).willReturn(Optional.of(RUNWAL));
        given(service.switchAddOn(new SwitchAddOnCommand(9, "FEED", true))).willReturn(Optional.empty());

        mockMvc.perform(put("/residency/societies/1/add-ons/COMPLAINTS/availability")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addOns[1].addOn").value("COMPLAINTS"))
                .andExpect(jsonPath("$.addOns[1].available").value(true));
        mockMvc.perform(put("/residency/societies/1/add-ons/FEED").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"on\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addOns[0].on").value(true))
                .andExpect(jsonPath("$.latitude").value(19.186062));
        mockMvc.perform(put("/residency/societies/9/add-ons/FEED").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"on\": true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void theAdminSwitchesComplaintsOffForOneTower() throws Exception {
        given(service.switchAddOnInTower(new SwitchAddOnInTowerCommand(1, "COMPLAINTS", 5, false)))
                .willReturn(Optional.of(RUNWAL));
        given(service.switchAddOnInTower(new SwitchAddOnInTowerCommand(1, "COMPLAINTS", 99, false)))
                .willReturn(Optional.empty());

        mockMvc.perform(put("/residency/societies/1/add-ons/COMPLAINTS/towers/5")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"on\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintsOffInTowers[0]").value(5));
        mockMvc.perform(put("/residency/societies/1/add-ons/COMPLAINTS/towers/99")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"on\": false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void refusalsAre403AndBrokenRulesAre422() throws Exception {
        given(service.offer(new OfferAddOnCommand(1, "FEED", true))).willThrow(new ActionNotAllowedException("no"));
        given(service.switchAddOn(new SwitchAddOnCommand(1, "COMPLAINTS", true)))
                .willThrow(new InvalidResidencyException("COMPLAINTS is not available to this society"));

        mockMvc.perform(put("/residency/societies/1/add-ons/FEED/availability")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\": true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/residency/societies/1/add-ons/COMPLAINTS").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"on\": true}"))
                .andExpect(status().is(422));
    }
}
