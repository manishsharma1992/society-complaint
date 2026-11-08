package com.mycity.runwal.complaint.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.complaint.application.command.SetStaffCategoriesCommand;
import com.mycity.runwal.complaint.application.command.SwitchAutoAssignmentCommand;
import com.mycity.runwal.complaint.application.query.AutoAssignmentView;
import com.mycity.runwal.complaint.application.query.GetAutoAssignmentQuery;
import com.mycity.runwal.complaint.application.service.ActionNotAllowedException;
import com.mycity.runwal.complaint.application.service.AutoAssignmentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AutoAssignmentController.class)
class AutoAssignmentControllerTest {

    private static final AutoAssignmentView ON = new AutoAssignmentView(true,
            List.of(new AutoAssignmentView.StaffCategoriesView(30, List.of(1L, 3L))));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutoAssignmentService service;

    @Test
    void readSwitchAndSetCategoriesUnderTheComplaintPrefix() throws Exception {
        given(service.view(new GetAutoAssignmentQuery(1))).willReturn(ON);
        given(service.switchOn(new SwitchAutoAssignmentCommand(1, true))).willReturn(ON);
        given(service.setCategories(new SetStaffCategoriesCommand(1, 30, List.of(1L, 3L)))).willReturn(ON);

        mockMvc.perform(get("/complaint/societies/1/auto-assignment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.on").value(true))
                .andExpect(jsonPath("$.staff[0].categoryIds[1]").value(3));
        mockMvc.perform(put("/complaint/societies/1/auto-assignment").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"on\": true}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/complaint/societies/1/auto-assignment/staff/30").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryIds\": [1, 3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staff[0].staffId").value(30));
    }

    @Test
    void notTheSocietyAdminIs403() throws Exception {
        given(service.switchOn(new SwitchAutoAssignmentCommand(1, false))).willThrow(new ActionNotAllowedException("no"));

        mockMvc.perform(put("/complaint/societies/1/auto-assignment").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"on\": false}"))
                .andExpect(status().isForbidden());
    }
}
