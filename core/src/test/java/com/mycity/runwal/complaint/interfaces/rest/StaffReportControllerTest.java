package com.mycity.runwal.complaint.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.complaint.application.query.StaffPerformanceQuery;
import com.mycity.runwal.complaint.application.query.StaffPerformanceView;
import com.mycity.runwal.complaint.application.service.ActionNotAllowedException;
import com.mycity.runwal.complaint.application.service.StaffReportService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StaffReportController.class)
class StaffReportControllerTest {

    private static final Instant FROM = Instant.parse("2026-10-31T18:30:00Z");
    private static final Instant TO = Instant.parse("2026-11-30T18:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaffReportService service;

    @Test
    void reportsPerStaffMemberUnderTheComplaintPrefix() throws Exception {
        given(service.report(new StaffPerformanceQuery(1, FROM, TO, 7L))).willReturn(List.of(
                new StaffPerformanceView(30, 3, 3, 2, 0, 120L, 1050L, 1, 1, 1)));

        mockMvc.perform(get("/complaint/societies/1/reports/staff")
                        .param("from", FROM.toString()).param("to", TO.toString()).param("towerId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staffId").value(30))
                .andExpect(jsonPath("$[0].medianMinutesToResolve").value(1050))
                .andExpect(jsonPath("$[0].overdueNow").value(1));
    }

    @Test
    void outsidersGet403AndAMissingPeriodIs400() throws Exception {
        given(service.report(new StaffPerformanceQuery(1, FROM, TO, null)))
                .willThrow(new ActionNotAllowedException("no"));

        mockMvc.perform(get("/complaint/societies/1/reports/staff")
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/complaint/societies/1/reports/staff")).andExpect(status().isBadRequest());
    }
}
