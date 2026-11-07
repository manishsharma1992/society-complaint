package com.mycity.runwal.workforce.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.workforce.application.port.ComplaintsAddOn;
import com.mycity.runwal.workforce.application.service.ShiftQueryService;
import com.mycity.runwal.workforce.application.service.ShiftReviewService;
import com.mycity.runwal.workforce.application.service.ShiftService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Staff shifts come with the Complaints add-on: without it, every /workforce/societies/{id} request is 403. */
@WebMvcTest(ShiftController.class)
class AddOnInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintsAddOn addOn;

    @MockitoBean
    private ShiftService shiftService;

    @MockitoBean
    private ShiftQueryService queryService;

    @MockitoBean
    private ShiftReviewService reviewService;

    @Test
    void offMeans403AndNothingIsCalled() throws Exception {
        given(addOn.isOnFor(new SocietyId(2))).willReturn(false);

        mockMvc.perform(get("/workforce/societies/2/shifts/on-duty")).andExpect(status().isForbidden());
        verifyNoInteractions(queryService);
    }

    @Test
    void onLetsTheRequestThrough() throws Exception {
        given(addOn.isOnFor(new SocietyId(1))).willReturn(true);

        mockMvc.perform(get("/workforce/societies/1/shifts/on-duty")).andExpect(status().isOk());
    }
}
