package com.mycity.runwal.complaint.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.complaint.application.port.ComplaintsAddOn;
import com.mycity.runwal.complaint.application.service.ComplaintCategoryService;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** A society that has switched the Complaints add-on off gets 403 for everything under /complaint/societies/{id}. */
@WebMvcTest(CategoryController.class)
class AddOnInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintsAddOn addOn;

    @MockitoBean
    private ComplaintCategoryService categories;

    @Test
    void offMeans403WithTheReasonAndTheServiceIsNeverCalled() throws Exception {
        given(addOn.isOnFor(new SocietyId(2))).willReturn(false);

        mockMvc.perform(get("/complaint/societies/2/categories"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("This society has not switched on the Complaints add-on"));
        verifyNoInteractions(categories);
    }

    @Test
    void onLetsTheRequestThrough() throws Exception {
        given(addOn.isOnFor(new SocietyId(1))).willReturn(true);
        given(categories.active(any())).willReturn(List.of());

        mockMvc.perform(get("/complaint/societies/1/categories")).andExpect(status().isOk());
    }
}
