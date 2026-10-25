package com.mycity.runwal.residency.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.residency.application.command.AddFlatMemberCommand;
import com.mycity.runwal.residency.application.command.RemoveFlatMemberCommand;
import com.mycity.runwal.residency.application.query.FlatMemberView;
import com.mycity.runwal.residency.application.query.FlatView;
import com.mycity.runwal.residency.application.query.FlatsOfMemberQuery;
import com.mycity.runwal.residency.application.query.GetFlatQuery;
import com.mycity.runwal.residency.application.query.GetSocietyQuery;
import com.mycity.runwal.residency.application.query.ListFlatsQuery;
import com.mycity.runwal.residency.application.query.ListTowersQuery;
import com.mycity.runwal.residency.application.query.SocietyView;
import com.mycity.runwal.residency.application.query.TowerView;
import com.mycity.runwal.residency.application.service.ActionNotAllowedException;
import com.mycity.runwal.residency.application.service.FlatMembershipService;
import com.mycity.runwal.residency.application.service.ResidencyQueryService;
import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ResidencyController.class, FlatMemberController.class})
class ResidencyControllersTest {

    private static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");
    private static final FlatView B2_503 = new FlatView(795, 7, "B2", 5, 503, "B2-503",
            List.of(new FlatMemberView(10, "OWNER", NOW)));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResidencyQueryService queryService;

    @MockitoBean
    private FlatMembershipService membershipService;

    @Test
    void societyAndTowersUnderTheResidencyPrefix() throws Exception {
        given(queryService.society(new GetSocietyQuery(1))).willReturn(Optional.of(new SocietyView(1, "Runwal MyCity")));
        given(queryService.towers(new ListTowersQuery(1))).willReturn(List.of(new TowerView(7, "B2", "Radiant", 27)));

        mockMvc.perform(get("/residency/societies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Runwal MyCity"));
        mockMvc.perform(get("/residency/societies/1/towers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("B2"))
                .andExpect(jsonPath("$[0].name").value("Radiant"))
                .andExpect(jsonPath("$[0].residentialFloors").value(27));
    }

    @Test
    void flatsOfTowerAndSingleFlat() throws Exception {
        given(queryService.flats(new ListFlatsQuery(1, 7))).willReturn(Optional.of(List.of(B2_503)));
        given(queryService.flat(new GetFlatQuery(1, 795))).willReturn(Optional.of(B2_503));

        mockMvc.perform(get("/residency/societies/1/towers/7/flats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("B2-503"));
        mockMvc.perform(get("/residency/societies/1/flats/795"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(795))
                .andExpect(jsonPath("$.towerId").value(7))
                .andExpect(jsonPath("$.number").value(503))
                .andExpect(jsonPath("$.members[0].type").value("OWNER"));
    }

    @Test
    void unknownTowerOrFlatIs404() throws Exception {
        given(queryService.flats(new ListFlatsQuery(1, 99))).willReturn(Optional.empty());
        given(queryService.flat(new GetFlatQuery(1, 99999))).willReturn(Optional.empty());

        mockMvc.perform(get("/residency/societies/1/towers/99/flats")).andExpect(status().isNotFound());
        mockMvc.perform(get("/residency/societies/1/flats/99999")).andExpect(status().isNotFound());
    }

    @Test
    void flatsOfAMember() throws Exception {
        given(queryService.flatsOfMember(new FlatsOfMemberQuery(1, 10))).willReturn(List.of(B2_503));

        mockMvc.perform(get("/residency/societies/1/flats").param("memberId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("B2-503"));
    }

    @Test
    void addAndRemoveMember() throws Exception {
        given(membershipService.addMember(new AddFlatMemberCommand(1, 795, 10, "OWNER")))
                .willReturn(Optional.of(B2_503));
        given(membershipService.removeMember(new RemoveFlatMemberCommand(1, 795, 10)))
                .willReturn(Optional.of(B2_503));

        mockMvc.perform(post("/residency/societies/1/flats/795/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 10, "type": "OWNER"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("B2-503"));
        mockMvc.perform(delete("/residency/societies/1/flats/795/members/10"))
                .andExpect(status().isOk());
    }

    @Test
    void notAllowedIs403() throws Exception {
        given(queryService.flatsOfMember(new FlatsOfMemberQuery(1, 11)))
                .willThrow(new ActionNotAllowedException("You can only see your own flats"));

        mockMvc.perform(get("/residency/societies/1/flats").param("memberId", "11"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only see your own flats"));
    }

    @Test
    void invalidMemberTypeIs400AndBrokenRuleIs422() throws Exception {
        mockMvc.perform(post("/residency/societies/1/flats/795/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 10, "type": "FAMILY"}"""))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(membershipService);

        given(membershipService.addMember(new AddFlatMemberCommand(1, 795, 10, "TENANT")))
                .willThrow(new InvalidResidencyException("User 10 is already linked to flat 795"));
        mockMvc.perform(post("/residency/societies/1/flats/795/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 10, "type": "TENANT"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("User 10 is already linked to flat 795"));
    }
}
