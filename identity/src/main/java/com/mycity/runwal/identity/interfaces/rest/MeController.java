package com.mycity.runwal.identity.interfaces.rest;

import com.mycity.runwal.identity.application.command.RegisterCommand;
import com.mycity.runwal.identity.application.service.RegistrationService;
import com.mycity.runwal.identity.application.service.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in user: own profile and self sign-up for a flat. */
@RestController
@RequestMapping("/me")
class MeController {

    private final RegistrationService registrationService;
    private final SessionService sessionService;

    MeController(RegistrationService registrationService, SessionService sessionService) {
        this.registrationService = registrationService;
        this.sessionService = sessionService;
    }

    @GetMapping
    ResponseEntity<UserResponse> me() {
        return ResponseEntity.of(registrationService.me().map(UserResponse::from));
    }

    @PostMapping("/registration")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.of(registrationService.register(new RegisterCommand(request.name(),
                        request.societyId(), request.flatId(), request.occupancy()))
                .map(UserResponse::from));
    }

    /** A fresh login token with the current status and roles (e.g. after approval), ending when this login ends. */
    @PostMapping("/session")
    ResponseEntity<LoginController.SessionResponse> renewSession() {
        return ResponseEntity.of(sessionService.renew().map(session -> new LoginController.SessionResponse(
                session.token(), session.expiresAt(), UserResponse.from(session.user()))));
    }

    record RegistrationRequest(@NotBlank @Size(max = 100) String name, @NotNull @Positive Long societyId,
                               @NotNull @Positive Long flatId,
                               @NotNull @Pattern(regexp = "OWNER|TENANT") String occupancy) {
    }
}
