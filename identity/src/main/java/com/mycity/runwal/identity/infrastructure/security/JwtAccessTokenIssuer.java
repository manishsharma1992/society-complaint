package com.mycity.runwal.identity.infrastructure.security;

import com.mycity.runwal.identity.application.port.AccessTokenIssuer;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.RoleAssignment;
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Signs login tokens (JWT, HS256). Claims: sub = user ID, status, roles as "ROLE:society" or "ROLE:society:tower",
 * platformOwner (true for whoever runs the platform).
 * The app verifies them with the same key.
 */
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;

    public JwtAccessTokenIssuer(SecretKey signingKey) {
        this.encoder = NimbusJwtEncoder.withSecretKey(signingKey).algorithm(MacAlgorithm.HS256).build();
    }

    @Override
    public String issue(AppUser user, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.id().value()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("status", user.status().name())
                .claim("roles", roles(user.roles()))
                .claim("platformOwner", user.isPlatformOwner())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    static List<String> roles(List<RoleAssignment> roles) {
        return roles.stream()
                .map(r -> r.role().name() + ":" + r.societyId().value()
                        + (r.towerId() == null ? "" : ":" + r.towerId().value()))
                .toList();
    }
}
