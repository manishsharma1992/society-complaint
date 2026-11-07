package com.mycity.runwal.identity.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.identity.application.command.ApproveRegistrationCommand;
import com.mycity.runwal.identity.application.command.GrantRoleCommand;
import com.mycity.runwal.identity.application.command.RegisterCommand;
import com.mycity.runwal.identity.application.command.RequestOtpCommand;
import com.mycity.runwal.identity.application.command.RevokeRoleCommand;
import com.mycity.runwal.identity.application.command.VerifyOtpCommand;
import com.mycity.runwal.identity.application.query.FlatClaimView;
import com.mycity.runwal.identity.application.query.ListPendingRegistrationsQuery;
import com.mycity.runwal.identity.application.query.OtpSentView;
import com.mycity.runwal.identity.application.query.RoleView;
import com.mycity.runwal.identity.application.query.SessionView;
import com.mycity.runwal.identity.application.query.UserView;
import com.mycity.runwal.identity.application.service.ActionNotAllowedException;
import com.mycity.runwal.identity.application.service.LoginFailedException;
import com.mycity.runwal.identity.application.service.OtpLoginService;
import com.mycity.runwal.identity.application.service.RegistrationService;
import com.mycity.runwal.identity.application.service.RoleService;
import com.mycity.runwal.identity.application.service.SessionService;
import com.mycity.runwal.identity.application.service.TooManyCodesException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({LoginController.class, MeController.class, RegistrationController.class, UserRoleController.class})
class IdentityControllersTest {

    private static final UserView PENDING = new UserView(100, "+919876543210", "Manish Sharma", "PENDING", List.of(),
            new FlatClaimView(1, 667, "OWNER"), null, false);
    private static final UserView ACTIVE = new UserView(100, "+919876543210", "Manish Sharma", "ACTIVE",
            List.of(new RoleView("RESIDENT", 1, null)), new FlatClaimView(1, 667, "OWNER"), null, false);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OtpLoginService loginService;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void requestAndVerifyCode() throws Exception {
        given(loginService.requestOtp(new RequestOtpCommand("98765 43210")))
                .willReturn(new OtpSentView("+91******3210", 300, 30));
        given(loginService.verify(new VerifyOtpCommand("98765 43210", "123456")))
                .willReturn(new SessionView("jwt", Instant.parse("2026-11-24T08:00:00Z"), ACTIVE));

        mockMvc.perform(post("/identity/login/otp").contentType(MediaType.APPLICATION_JSON).content("""
                        {"phone": "98765 43210"}"""))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sentTo").value("+91******3210"))
                .andExpect(jsonPath("$.resendAfterSeconds").value(30));
        mockMvc.perform(post("/identity/login/verify").contentType(MediaType.APPLICATION_JSON).content("""
                        {"phone": "98765 43210", "code": "123456"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.user.roles[0].role").value("RESIDENT"));
    }

    @Test
    void wrongCodeIs401AndTooManyCodesIs429() throws Exception {
        given(loginService.verify(any())).willThrow(new LoginFailedException());
        given(loginService.requestOtp(any())).willThrow(new TooManyCodesException("Please wait 20 seconds"));

        mockMvc.perform(post("/identity/login/verify").contentType(MediaType.APPLICATION_JSON).content("""
                        {"phone": "98765 43210", "code": "000000"}"""))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/identity/login/otp").contentType(MediaType.APPLICATION_JSON).content("""
                        {"phone": "98765 43210"}"""))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("Please wait 20 seconds"));
    }

    @Test
    void codeMustBeSixDigits() throws Exception {
        mockMvc.perform(post("/identity/login/verify").contentType(MediaType.APPLICATION_JSON).content("""
                        {"phone": "98765 43210", "code": "12ab"}"""))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(loginService);
    }

    @Test
    void meAndSelfSignUp() throws Exception {
        given(registrationService.me()).willReturn(Optional.of(PENDING));
        given(registrationService.register(new RegisterCommand("Manish Sharma", 1, 667, "OWNER")))
                .willReturn(Optional.of(PENDING));

        mockMvc.perform(get("/identity/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        mockMvc.perform(post("/identity/me/registration").contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "Manish Sharma", "societyId": 1, "flatId": 667, "occupancy": "OWNER"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claim.flatId").value(667));
    }

    @Test
    void renewSessionAfterApproval() throws Exception {
        given(sessionService.renew()).willReturn(Optional.of(new SessionView("jwt2", Instant.parse("2026-11-24T08:00:00Z"), ACTIVE)));

        mockMvc.perform(post("/identity/me/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt2"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-24T08:00:00Z"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));
    }

    @Test
    void reviewRegistrations() throws Exception {
        given(registrationService.pending(new ListPendingRegistrationsQuery(1))).willReturn(List.of(PENDING));
        given(registrationService.approve(new ApproveRegistrationCommand(1, 100))).willReturn(Optional.of(ACTIVE));
        given(registrationService.approve(new ApproveRegistrationCommand(1, 404))).willReturn(Optional.empty());

        mockMvc.perform(get("/identity/societies/1/registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Manish Sharma"));
        mockMvc.perform(post("/identity/societies/1/registrations/100/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(post("/identity/societies/1/registrations/404/approve"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/identity/societies/1/registrations/100/reject").contentType(MediaType.APPLICATION_JSON).content("""
                                {"reason": " "}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notAllowedIs403() throws Exception {
        given(registrationService.pending(new ListPendingRegistrationsQuery(1)))
                .willThrow(new ActionNotAllowedException("Only the society admin can see pending registrations"));

        mockMvc.perform(get("/identity/societies/1/registrations"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Not allowed"));
    }

    @Test
    void grantAndRevokeRoles() throws Exception {
        given(roleService.grant(new GrantRoleCommand(1, 100, "TOWER_ADMIN", 7L))).willReturn(Optional.of(ACTIVE));
        given(roleService.revoke(new RevokeRoleCommand(1, 100, "TOWER_ADMIN", 7L))).willReturn(Optional.of(ACTIVE));

        mockMvc.perform(post("/identity/societies/1/users/100/roles").contentType(MediaType.APPLICATION_JSON).content("""
                                {"role": "TOWER_ADMIN", "towerId": 7}"""))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/identity/societies/1/users/100/roles").param("role", "TOWER_ADMIN").param("towerId", "7"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/identity/societies/1/users/100/roles").contentType(MediaType.APPLICATION_JSON).content("""
                                {"role": "GUARD"}"""))
                .andExpect(status().isBadRequest());
    }
}
