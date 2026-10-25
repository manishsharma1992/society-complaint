package com.mycity.runwal.identity.interfaces.rest;

import com.mycity.runwal.identity.application.command.RequestOtpCommand;
import com.mycity.runwal.identity.application.command.VerifyOtpCommand;
import com.mycity.runwal.identity.application.query.OtpSentView;
import com.mycity.runwal.identity.application.query.SessionView;
import com.mycity.runwal.identity.application.service.OtpLoginService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Phone + OTP login: request a code, then exchange it for a login token. */
@RestController
@RequestMapping("/login")
class LoginController {

    private final OtpLoginService loginService;

    LoginController(OtpLoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/otp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    OtpSentResponse requestOtp(@Valid @RequestBody OtpRequest request) {
        OtpSentView sent = loginService.requestOtp(new RequestOtpCommand(request.phone()));
        return new OtpSentResponse(sent.sentTo(), sent.expiresInSeconds(), sent.resendAfterSeconds());
    }

    @PostMapping("/verify")
    SessionResponse verify(@Valid @RequestBody VerifyRequest request) {
        SessionView session = loginService.verify(new VerifyOtpCommand(request.phone(), request.code()));
        return new SessionResponse(session.token(), session.expiresAt(), UserResponse.from(session.user()));
    }

    record OtpRequest(@NotBlank @Size(max = 20) String phone) {
    }

    record VerifyRequest(@NotBlank @Size(max = 20) String phone, @NotBlank @Pattern(regexp = "\\d{6}") String code) {
    }

    record OtpSentResponse(String sentTo, long expiresInSeconds, long resendAfterSeconds) {
    }

    /** Send {@code token} as "Authorization: Bearer ..." on every request. */
    record SessionResponse(String token, Instant expiresAt, UserResponse user) {
    }
}
