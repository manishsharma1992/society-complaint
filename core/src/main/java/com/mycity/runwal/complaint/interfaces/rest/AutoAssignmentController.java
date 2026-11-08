package com.mycity.runwal.complaint.interfaces.rest;

import com.mycity.runwal.complaint.application.command.SetStaffCategoriesCommand;
import com.mycity.runwal.complaint.application.command.SwitchAutoAssignmentCommand;
import com.mycity.runwal.complaint.application.query.AutoAssignmentView;
import com.mycity.runwal.complaint.application.query.GetAutoAssignmentQuery;
import com.mycity.runwal.complaint.application.service.AutoAssignmentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Automatic assignment: whether it is on (every member may know, so residents can be told their complaint waits),
 * and, for admins, who handles which categories. The society admin changes both.
 */
@RestController
@RequestMapping("/societies/{societyId}/auto-assignment")
class AutoAssignmentController {

    private final AutoAssignmentService service;

    AutoAssignmentController(AutoAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    AutoAssignmentResponse get(@PathVariable long societyId) {
        return AutoAssignmentResponse.from(service.view(new GetAutoAssignmentQuery(societyId)));
    }

    @PutMapping
    AutoAssignmentResponse switchOn(@PathVariable long societyId, @RequestBody SwitchRequest request) {
        return AutoAssignmentResponse.from(service.switchOn(new SwitchAutoAssignmentCommand(societyId, request.on())));
    }

    @PutMapping("/staff/{staffId}")
    AutoAssignmentResponse setCategories(@PathVariable long societyId, @PathVariable long staffId,
                                         @RequestBody CategoriesRequest request) {
        return AutoAssignmentResponse.from(service.setCategories(
                new SetStaffCategoriesCommand(societyId, staffId, request.categoryIds() == null ? List.of() : request.categoryIds())));
    }

    record SwitchRequest(boolean on) {
    }

    record CategoriesRequest(List<Long> categoryIds) {
    }

    record AutoAssignmentResponse(boolean on, List<StaffResponse> staff) {

        static AutoAssignmentResponse from(AutoAssignmentView view) {
            return new AutoAssignmentResponse(view.on(),
                    view.staff().stream().map(s -> new StaffResponse(s.staffId(), s.categoryIds())).toList());
        }
    }

    record StaffResponse(long staffId, List<Long> categoryIds) {
    }
}
