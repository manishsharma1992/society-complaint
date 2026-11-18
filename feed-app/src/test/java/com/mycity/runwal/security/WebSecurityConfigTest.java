package com.mycity.runwal.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.feed.application.port.FeedAddOn;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Runs the real security filter chain in front of two test endpoints. */
@WebMvcTest(WebSecurityConfigTest.ProbeController.class)
@Import({WebSecurityConfig.class, WebSecurityConfigTest.Keys.class, WebSecurityConfigTest.ProbeController.class})
class WebSecurityConfigTest {

    private static final SecretKey KEY = key("0123456789abcdef0123456789abcdef");

    @Autowired
    private MockMvc mockMvc;

    /** The feed's web config asks its add-on port; the integration adapter behind it is not in this slice. */
    @MockitoBean
    private FeedAddOn feedAddOn;

    @Test
    void loginEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/identity/login/probe")).andExpect(status().isOk());
    }

    @Test
    void thePublicFeedIsOpenToEveryoneWithOrWithoutALogin() throws Exception {
        mockMvc.perform(get("/feed/public/probe")).andExpect(status().isOk());
        mockMvc.perform(get("/feed/share/probe")).andExpect(status().isOk());
        mockMvc.perform(get("/feed/public/probe").header("Authorization", "Bearer " + token(KEY, Instant.now().plusSeconds(60))))
                .andExpect(status().isOk());
    }

    @Test
    void everythingElseNeedsAToken() throws Exception {
        mockMvc.perform(get("/feed/societies/1/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenIdentifiesTheUser() throws Exception {
        mockMvc.perform(get("/feed/societies/1/probe").header("Authorization", "Bearer " + token(KEY, Instant.now().plusSeconds(60))))
                .andExpect(status().isOk())
                .andExpect(content().string("user 4"));
    }

    @Test
    void forgedOrExpiredTokensAreRefused() throws Exception {
        String forged = token(key("an-attacker-key-an-attacker-key-x"), Instant.now().plusSeconds(60));
        String expired = token(KEY, Instant.now().minusSeconds(120));

        mockMvc.perform(get("/feed/societies/1/probe").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/feed/societies/1/probe").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    private static String token(SecretKey key, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("4").issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt).claim("status", "ACTIVE").build();
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build()
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @TestConfiguration
    static class Keys {

        @Bean
        SecretKey tokenSigningKey() {
            return KEY;
        }
    }

    @RestController
    static class ProbeController {

        @GetMapping("/identity/login/probe")
        String publicProbe() {
            return "public";
        }

        @GetMapping({"/feed/public/probe", "/feed/share/probe"})
        String publicFeedProbe() {
            return "public";
        }

        @GetMapping("/feed/societies/1/probe")
        String protectedProbe(@AuthenticationPrincipal Jwt jwt) {
            return "user " + jwt.getSubject();
        }
    }
}
