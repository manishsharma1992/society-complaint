package com.mycity.runwal.workforce.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.workforce.application.command.EndShiftCommand;
import com.mycity.runwal.workforce.application.command.StartShiftCommand;
import com.mycity.runwal.workforce.application.query.OnDutyQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursView;
import com.mycity.runwal.workforce.application.query.ShiftView;
import com.mycity.runwal.workforce.application.service.ActionNotAllowedException;
import com.mycity.runwal.workforce.application.service.NoOpenShiftException;
import com.mycity.runwal.workforce.application.service.ShiftAlreadyOpenException;
import com.mycity.runwal.workforce.application.service.ShiftQueryService;
import com.mycity.runwal.workforce.application.service.ShiftService;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShiftController.class)
class ShiftControllerTest {

    private static final Instant SIX = Instant.parse("2026-11-07T00:30:00Z");
    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-12-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShiftService shiftService;

    @MockitoBean
    private ShiftQueryService queryService;

    @Test
    void staffClockInAndOutUnderTheWorkforcePrefix() throws Exception {
        given(shiftService.start(new StartShiftCommand(1))).willReturn(new ShiftView(5, 30, SIX, null, 0));
        given(shiftService.end(new EndShiftCommand(1))).willReturn(new ShiftView(5, 30, SIX, SIX.plusSeconds(3600), 60));

        mockMvc.perform(post("/workforce/societies/1/shifts/start"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.staffId").value(30))
                .andExpect(jsonPath("$.endedAt").doesNotExist());
        mockMvc.perform(post("/workforce/societies/1/shifts/end"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutesCounted").value(60));
    }

    @Test
    void adminsReadOnDutyAndHours() throws Exception {
        given(queryService.onDuty(new OnDutyQuery(1))).willReturn(List.of(new ShiftView(5, 30, SIX, null, 90)));
        given(queryService.hours(new ShiftHoursQuery(1, FROM, TO)))
                .willReturn(List.of(new ShiftHoursView(30, 20, 9600, true)));

        mockMvc.perform(get("/workforce/societies/1/shifts/on-duty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staffId").value(30));
        mockMvc.perform(get("/workforce/societies/1/shifts/hours")
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].minutesWorked").value(9600))
                .andExpect(jsonPath("$[0].onDutyNow").value(true));
    }

    @Test
    void refusalsMapToClearStatusCodes() throws Exception {
        given(shiftService.start(new StartShiftCommand(1))).willThrow(new ShiftAlreadyOpenException());
        given(shiftService.end(new EndShiftCommand(1))).willThrow(new NoOpenShiftException());
        given(queryService.onDuty(new OnDutyQuery(1))).willThrow(new ActionNotAllowedException("no"));
        given(queryService.hours(new ShiftHoursQuery(1, TO, FROM))).willThrow(new InvalidShiftException("backwards"));

        mockMvc.perform(post("/workforce/societies/1/shifts/start")).andExpect(status().isConflict());
        mockMvc.perform(post("/workforce/societies/1/shifts/end")).andExpect(status().isNotFound());
        mockMvc.perform(get("/workforce/societies/1/shifts/on-duty")).andExpect(status().isForbidden());
        mockMvc.perform(get("/workforce/societies/1/shifts/hours")
                        .param("from", TO.toString()).param("to", FROM.toString()))
                .andExpect(status().is(422));
        mockMvc.perform(get("/workforce/societies/1/shifts/hours")).andExpect(status().isBadRequest());
    }
}
