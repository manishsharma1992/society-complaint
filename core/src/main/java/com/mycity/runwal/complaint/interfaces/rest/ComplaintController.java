package com.mycity.runwal.complaint.interfaces.rest;

import com.mycity.runwal.complaint.application.command.AssignComplaintCommand;
import com.mycity.runwal.complaint.application.command.CloseComplaintCommand;
import com.mycity.runwal.complaint.application.command.RejectComplaintCommand;
import com.mycity.runwal.complaint.application.command.ResolveComplaintCommand;
import com.mycity.runwal.complaint.application.command.StartWorkCommand;
import com.mycity.runwal.complaint.application.query.ComplaintView;
import com.mycity.runwal.complaint.application.query.GetComplaintQuery;
import com.mycity.runwal.complaint.application.query.ListComplaintsQuery;
import com.mycity.runwal.complaint.application.service.ComplaintLifecycleService;
import com.mycity.runwal.complaint.application.service.ComplaintQueryService;
import com.mycity.runwal.complaint.application.service.RaiseComplaintService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/societies/{societyId}/complaints")
class ComplaintController {

    private final RaiseComplaintService raiseComplaintService;
    private final ComplaintQueryService complaintQueryService;
    private final ComplaintLifecycleService lifecycleService;

    ComplaintController(RaiseComplaintService raiseComplaintService, ComplaintQueryService complaintQueryService,
                        ComplaintLifecycleService lifecycleService) {
        this.raiseComplaintService = raiseComplaintService;
        this.complaintQueryService = complaintQueryService;
        this.lifecycleService = lifecycleService;
    }

    @PostMapping
    ResponseEntity<ComplaintResponse> raise(@PathVariable long societyId,
                                            @Valid @RequestBody RaiseComplaintRequest request) {
        ComplaintView complaint = raiseComplaintService.raise(request.toCommand(societyId));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{complaintId}")
                .buildAndExpand(complaint.id())
                .toUri();
        return ResponseEntity.created(location).body(ComplaintResponse.from(complaint));
    }

    @GetMapping
    List<ComplaintResponse> list(@PathVariable long societyId,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) Long towerId,
                                 @RequestParam(required = false) Long assignedTo,
                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return complaintQueryService.list(new ListComplaintsQuery(societyId, status, towerId, assignedTo, page, size))
                .stream()
                .map(ComplaintResponse::from)
                .toList();
    }

    @GetMapping("/{complaintId}")
    ResponseEntity<ComplaintResponse> get(@PathVariable long societyId, @PathVariable long complaintId) {
        return ResponseEntity.of(complaintQueryService.get(new GetComplaintQuery(societyId, complaintId))
                .map(ComplaintResponse::from));
    }

    @PostMapping("/{complaintId}/assign")
    ResponseEntity<ComplaintResponse> assign(@PathVariable long societyId, @PathVariable long complaintId,
                                             @Valid @RequestBody AssignComplaintRequest request) {
        return ResponseEntity.of(lifecycleService.assign(
                new AssignComplaintCommand(societyId, complaintId, request.staffId()))
                .map(ComplaintResponse::from));
    }

    @PostMapping("/{complaintId}/start")
    ResponseEntity<ComplaintResponse> startWork(@PathVariable long societyId, @PathVariable long complaintId) {
        return ResponseEntity.of(lifecycleService.startWork(new StartWorkCommand(societyId, complaintId))
                .map(ComplaintResponse::from));
    }

    /** The body is optional: {"note": "..."}. */
    @PostMapping("/{complaintId}/resolve")
    ResponseEntity<ComplaintResponse> resolve(@PathVariable long societyId, @PathVariable long complaintId,
                                              @Valid @RequestBody(required = false) ResolveComplaintRequest request) {
        String note = request == null ? null : request.note();
        return ResponseEntity.of(lifecycleService.resolve(
                new ResolveComplaintCommand(societyId, complaintId, note))
                .map(ComplaintResponse::from));
    }

    @PostMapping("/{complaintId}/close")
    ResponseEntity<ComplaintResponse> close(@PathVariable long societyId, @PathVariable long complaintId) {
        return ResponseEntity.of(lifecycleService.close(new CloseComplaintCommand(societyId, complaintId))
                .map(ComplaintResponse::from));
    }

    @PostMapping("/{complaintId}/reject")
    ResponseEntity<ComplaintResponse> reject(@PathVariable long societyId, @PathVariable long complaintId,
                                             @Valid @RequestBody RejectComplaintRequest request) {
        return ResponseEntity.of(lifecycleService.reject(
                new RejectComplaintCommand(societyId, complaintId, request.reason()))
                .map(ComplaintResponse::from));
    }
}
