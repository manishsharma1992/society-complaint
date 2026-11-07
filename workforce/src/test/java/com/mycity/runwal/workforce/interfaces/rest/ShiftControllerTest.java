package com.mycity.runwal.workforce.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.workforce.application.command.EndShiftCommand;
import com.mycity.runwal.workforce.application.command.RequestSelfieUploadCommand;
import com.mycity.runwal.workforce.application.command.ReviewShiftCommand;
import com.mycity.runwal.workforce.application.command.StartShiftCommand;
import com.mycity.runwal.workforce.application.query.ClockEvidenceView;
import com.mycity.runwal.workforce.application.query.OnDutyQuery;
import com.mycity.runwal.workforce.application.query.SelfieUploadView;
import com.mycity.runwal.workforce.application.query.ShiftHoursQuery;
import com.mycity.runwal.workforce.application.query.ShiftHoursView;
import com.mycity.runwal.workforce.application.query.ShiftReviewView;
import com.mycity.runwal.workforce.application.query.ShiftView;
import com.mycity.runwal.workforce.application.query.ShiftsQuery;
import com.mycity.runwal.workforce.application.service.ActionNotAllowedException;
import com.mycity.runwal.workforce.application.service.NoOpenShiftException;
import com.mycity.runwal.workforce.application.service.ShiftAlreadyOpenException;
import com.mycity.runwal.workforce.application.service.ShiftQueryService;
import com.mycity.runwal.workforce.application.service.ShiftReviewService;
import com.mycity.runwal.workforce.application.service.ShiftService;
import com.mycity.runwal.workforce.domain.exception.InvalidShiftException;
import com.mycity.runwal.workforce.domain.exception.SelfieNotUploadedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShiftController.class)
class ShiftControllerTest {

    private static final Instant SIX = Instant.parse("2026-11-07T00:30:00Z");
    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-12-01T00:00:00Z");
    private static final String KEY = "shifts/1/30/1793925000000-abcdef12.jpg";
    private static final ClockEvidenceView INSIDE = new ClockEvidenceView("INSIDE", 52L, 19.1865, 73.075, 12.0,
            "https://bucket.test/" + KEY + "?get");
    private static final ShiftView OPEN = new ShiftView(5, 30, SIX, null, 0, INSIDE, null, null, false);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShiftService shiftService;

    @MockitoBean
    private ShiftQueryService queryService;

    @MockitoBean
    private ShiftReviewService reviewService;

    @Test
    void staffGetAnUploadLinkThenClockInAndOutWithSelfieAndLocation() throws Exception {
        given(shiftService.requestSelfieUpload(new RequestSelfieUploadCommand(1, 150_000)))
                .willReturn(new SelfieUploadView(KEY, "https://bucket.test/put", "image/jpeg", 150_000, SIX));
        given(shiftService.start(new StartShiftCommand(1, KEY, 19.1865, 73.075, 12.0))).willReturn(OPEN);
        given(shiftService.end(new EndShiftCommand(1, KEY, null, null, null))).willReturn(new ShiftView(5, 30, SIX,
                SIX.plusSeconds(3600), 60, INSIDE, new ClockEvidenceView("NO_LOCATION", null, null, null, null, "u"),
                null, true));

        mockMvc.perform(post("/workforce/societies/1/shifts/selfie-upload").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sizeBytes\": 150000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storageKey").value(KEY))
                .andExpect(jsonPath("$.method").value("PUT"))
                .andExpect(jsonPath("$.headers['Content-Type']").value("image/jpeg"));
        mockMvc.perform(post("/workforce/societies/1/shifts/start").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selfieKey\": \"" + KEY + "\", \"latitude\": 19.1865, \"longitude\": 73.075, \"accuracyMeters\": 12}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.start.placement").value("INSIDE"))
                .andExpect(jsonPath("$.start.distanceMeters").value(52))
                .andExpect(jsonPath("$.endedAt").doesNotExist());
        mockMvc.perform(post("/workforce/societies/1/shifts/end").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selfieKey\": \"" + KEY + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.end.placement").value("NO_LOCATION"))
                .andExpect(jsonPath("$.needsAttention").value(true));
    }

    @Test
    void adminsListReviewAndReadOnDutyAndHours() throws Exception {
        ShiftView reviewed = new ShiftView(5, 30, SIX, SIX.plusSeconds(3600), 60, INSIDE, INSIDE,
                new ShiftReviewView("DISPUTED", 2, SIX.plusSeconds(4000), "Not Raju"), false);
        given(queryService.list(new ShiftsQuery(1, FROM, TO))).willReturn(List.of(reviewed));
        given(reviewService.review(new ReviewShiftCommand(1, 5, ReviewShiftCommand.Decision.DISPUTE, "Not Raju")))
                .willReturn(Optional.of(reviewed));
        given(reviewService.review(new ReviewShiftCommand(1, 9, ReviewShiftCommand.Decision.CONFIRM, null)))
                .willReturn(Optional.empty());
        given(queryService.onDuty(new OnDutyQuery(1))).willReturn(List.of(OPEN));
        given(queryService.hours(new ShiftHoursQuery(1, FROM, TO)))
                .willReturn(List.of(new ShiftHoursView(30, 20, 9600, true)));

        mockMvc.perform(get("/workforce/societies/1/shifts").param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].start.selfieUrl").value("https://bucket.test/" + KEY + "?get"))
                .andExpect(jsonPath("$[0].review.decision").value("DISPUTED"));
        mockMvc.perform(post("/workforce/societies/1/shifts/5/review").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"DISPUTE\", \"reason\": \"Not Raju\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review.reason").value("Not Raju"));
        mockMvc.perform(post("/workforce/societies/1/shifts/9/review").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"CONFIRM\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/workforce/societies/1/shifts/on-duty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staffId").value(30));
        mockMvc.perform(get("/workforce/societies/1/shifts/hours")
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].minutesWorked").value(9600));
    }

    @Test
    void refusalsMapToClearStatusCodes() throws Exception {
        given(shiftService.start(new StartShiftCommand(1, KEY, null, null, null))).willThrow(new ShiftAlreadyOpenException());
        given(shiftService.end(new EndShiftCommand(1, KEY, null, null, null))).willThrow(new NoOpenShiftException());
        given(shiftService.start(new StartShiftCommand(1, "missing", null, null, null)))
                .willThrow(new SelfieNotUploadedException("missing"));
        given(queryService.onDuty(new OnDutyQuery(1))).willThrow(new ActionNotAllowedException("no"));
        given(queryService.hours(new ShiftHoursQuery(1, TO, FROM))).willThrow(new InvalidShiftException("backwards"));
        String body = "{\"selfieKey\": \"" + KEY + "\"}";

        mockMvc.perform(post("/workforce/societies/1/shifts/start").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/workforce/societies/1/shifts/end").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/workforce/societies/1/shifts/start").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selfieKey\": \"missing\"}"))
                .andExpect(status().is(422));
        mockMvc.perform(post("/workforce/societies/1/shifts/5/review").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"MAYBE\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/workforce/societies/1/shifts/on-duty")).andExpect(status().isForbidden());
        mockMvc.perform(get("/workforce/societies/1/shifts/hours")
                        .param("from", TO.toString()).param("to", FROM.toString()))
                .andExpect(status().is(422));
        mockMvc.perform(get("/workforce/societies/1/shifts/hours")).andExpect(status().isBadRequest());
    }
}
