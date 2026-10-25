package com.mycity.runwal.identity.interfaces.rest;

import com.mycity.runwal.identity.application.command.ApproveRegistrationCommand;
import com.mycity.runwal.identity.application.command.RejectRegistrationCommand;
import com.mycity.runwal.identity.application.query.ListPendingRegistrationsQuery;
import com.mycity.runwal.identity.application.service.RegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The society reviews self sign-ups: list pending ones, approve or reject. */
@RestController
@RequestMapping("/societies/{societyId}/registrations")
class RegistrationController {

    private final RegistrationService registrationService;

    RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    List<UserResponse> pending(@PathVariable long societyId) {
        return registrationService.pending(new ListPendingRegistrationsQuery(societyId)).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping("/{userId}/approve")
    ResponseEntity<UserResponse> approve(@PathVariable long societyId, @PathVariable long userId) {
        return ResponseEntity.of(registrationService.approve(new ApproveRegistrationCommand(societyId, userId))
                .map(UserResponse::from));
    }

    @PostMapping("/{userId}/reject")
    ResponseEntity<UserResponse> reject(@PathVariable long societyId, @PathVariable long userId,
                                        @Valid @RequestBody RejectionRequest request) {
        return ResponseEntity.of(registrationService.reject(
                        new RejectRegistrationCommand(societyId, userId, request.reason()))
                .map(UserResponse::from));
    }

    record RejectionRequest(@NotBlank @Size(max = 500) String reason) {
    }
}
