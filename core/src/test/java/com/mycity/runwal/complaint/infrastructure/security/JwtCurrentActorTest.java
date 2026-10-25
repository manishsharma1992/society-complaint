package com.mycity.runwal.complaint.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.actor.ActorRole;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtCurrentActorTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenClaimsBecomeAnActor() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("2").claim("status", "ACTIVE")
                .claim("roles", List.of("TOWER_ADMIN:1:7", "RESIDENT:1", "SOMETHING_NEW:1"))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        Actor actor = new JwtCurrentActor().actor();

        assertThat(actor.userId()).isEqualTo(new UserId(2));
        assertThat(actor.active()).isTrue();
        assertThat(actor.roles()).containsExactlyInAnyOrder(
                new ActorRole(ActorRole.Part.TOWER_ADMIN, new SocietyId(1), new TowerId(7)),
                new ActorRole(ActorRole.Part.RESIDENT, new SocietyId(1), null));
    }

    @Test
    void pendingUserIsNotActive() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("100").claim("status", "PENDING")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();

        Actor actor = JwtCurrentActor.toActor(jwt);

        assertThat(actor.active()).isFalse();
        assertThat(actor.roles()).isEmpty();
    }
}
