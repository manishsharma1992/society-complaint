package com.mycity.runwal.security;

import javax.crypto.SecretKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every request needs a valid login token ("Authorization: Bearer ...", issued by the Identity context),
 * except requesting and verifying a login code and the read-only public feed (published posts, and reporting
 * them). What a user may do is decided inside each context.
 * Stateless: no sessions or cookies, so no CSRF protection is needed.
 */
@Configuration
class WebSecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/identity/login/**", "/feed/public/**", "/feed/share/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .build();
    }

    /** Checks signature (same key that signs the tokens) and expiry. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey tokenSigningKey) {
        return NimbusJwtDecoder.withSecretKey(tokenSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
