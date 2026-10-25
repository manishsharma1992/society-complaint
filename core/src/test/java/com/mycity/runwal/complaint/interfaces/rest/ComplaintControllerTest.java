package com.mycity.runwal.complaint.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.complaint.application.command.AssignComplaintCommand;
import com.mycity.runwal.complaint.application.command.RaiseComplaintCommand;
import com.mycity.runwal.complaint.application.command.RejectComplaintCommand;
import com.mycity.runwal.complaint.application.command.ResolveComplaintCommand;
import com.mycity.runwal.complaint.application.command.StartWorkCommand;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.application.query.GetComplaintQuery;
import com.mycity.runwal.complaint.application.query.ListComplaintsQuery;
import com.mycity.runwal.complaint.application.query.StatusChangeView;
import com.mycity.runwal.complaint.application.service.ActionNotAllowedException;
import com.mycity.runwal.complaint.application.service.ComplaintLifecycleService;
import com.mycity.runwal.complaint.application.service.ComplaintQueryService;
import com.mycity.runwal.complaint.application.service.RaiseComplaintService;
import com.mycity.runwal.shared.ddd.DomainException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ComplaintController.class)
class ComplaintControllerTest {

    private static final Instant RAISED_AT = Instant.parse("2026-10-24T08:00:00Z");
    private static final ComplaintView VIEW = new ComplaintView(7, 1, "FLAT", 3L, 1204L, 5, "Lift stuck", null,
            "OPEN", 10, null, RAISED_AT, RAISED_AT.plusSeconds(4 * 3600),
            List.of(new StatusChangeView(null, "OPEN", 10, RAISED_AT, null)), List.of(), List.of());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RaiseComplaintService raiseComplaintService;

    @MockitoBean
    private ComplaintQueryService complaintQueryService;

    @MockitoBean
    private ComplaintLifecycleService lifecycleService;

    @Test
    void raisesComplaint() throws Exception {
        given(raiseComplaintService.raise(new RaiseComplaintCommand(1, "FLAT", 1204L, null, 5, "Lift stuck", null)))
                .willReturn(VIEW);

        mockMvc.perform(post("/complaint/societies/1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope": "FLAT", "flatId": 1204, "categoryId": 5, "title": "Lift stuck"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/complaint/societies/1/complaints/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.raisedAt").value("2026-10-24T08:00:00Z"))
                .andExpect(jsonPath("$.history[0].toStatus").value("OPEN"));
    }

    @Test
    void invalidRequestIsRejectedBeforeReachingTheApplication() throws Exception {
        mockMvc.perform(post("/complaint/societies/1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope": "HOUSE", "categoryId": 5, "title": " "}"""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(raiseComplaintService);
    }

    @Test
    void brokenBusinessRuleIs422() throws Exception {
        given(raiseComplaintService.raise(any())).willThrow(new DomainException("A flat complaint needs a flat") { });

        mockMvc.perform(post("/complaint/societies/1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope": "FLAT", "categoryId": 5, "title": "Leak"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("A flat complaint needs a flat"));
    }

    @Test
    void listsComplaintsWithFilters() throws Exception {
        given(complaintQueryService.list(new ListComplaintsQuery(1, "OPEN", 3L, 30L, 0, 20))).willReturn(List.of(VIEW));

        mockMvc.perform(get("/complaint/societies/1/complaints").param("status", "OPEN").param("towerId", "3")
                        .param("assignedTo", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    void pageSizeIsLimited() throws Exception {
        mockMvc.perform(get("/complaint/societies/1/complaints").param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownComplaintIs404() throws Exception {
        given(complaintQueryService.get(new GetComplaintQuery(1, 99))).willReturn(Optional.empty());

        mockMvc.perform(get("/complaint/societies/1/complaints/99")).andExpect(status().isNotFound());
    }

    @Test
    void assignsComplaint() throws Exception {
        given(lifecycleService.assign(new AssignComplaintCommand(1, 7, 20))).willReturn(Optional.of(VIEW));

        mockMvc.perform(post("/complaint/societies/1/complaints/7/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"staffId": 20}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void assignNeedsStaffId() throws Exception {
        mockMvc.perform(post("/complaint/societies/1/complaints/7/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lifecycleService);
    }

    @Test
    void resolvesWithoutBody() throws Exception {
        given(lifecycleService.resolve(new ResolveComplaintCommand(1, 7, null))).willReturn(Optional.of(VIEW));

        mockMvc.perform(post("/complaint/societies/1/complaints/7/resolve"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectNeedsReason() throws Exception {
        mockMvc.perform(post("/complaint/societies/1/complaints/7/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": " "}"""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lifecycleService);
    }

    @Test
    void rejectsComplaint() throws Exception {
        given(lifecycleService.reject(new RejectComplaintCommand(1, 7, "Duplicate"))).willReturn(Optional.of(VIEW));

        mockMvc.perform(post("/complaint/societies/1/complaints/7/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Duplicate"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void changingUnknownComplaintIs404() throws Exception {
        given(lifecycleService.startWork(new StartWorkCommand(1, 99))).willReturn(Optional.empty());

        mockMvc.perform(post("/complaint/societies/1/complaints/99/start"))
                .andExpect(status().isNotFound());
    }

    @Test
    void notAllowedIs403() throws Exception {
        given(lifecycleService.assign(any()))
                .willThrow(new ActionNotAllowedException("Only the society admin or this tower's admin can assign"));

        mockMvc.perform(post("/complaint/societies/1/complaints/7/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"staffId": 20}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only the society admin or this tower's admin can assign"));
    }

    @Test
    void invalidTransitionIs422() throws Exception {
        given(lifecycleService.close(any())).willThrow(new DomainException("Cannot move from OPEN to CLOSED") { });

        mockMvc.perform(post("/complaint/societies/1/complaints/7/close"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Cannot move from OPEN to CLOSED"));
    }
}
