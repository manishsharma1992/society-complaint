package com.mycity.runwal.residency.infrastructure.security;

import com.mycity.runwal.residency.application.port.CurrentActor;
import com.mycity.runwal.residency.domain.model.actor.Actor;
import com.mycity.runwal.residency.domain.model.actor.ActorRole;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Anticorruption layer to the Identity context's login token: translates its claims (sub, status,
 * roles "ROLE:society[:tower]") into this context's {@link Actor}. The token was already verified by the app.
 */
@Component("residencyCurrentActor")
class JwtCurrentActor implements CurrentActor {

    @Override
    public Actor actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No logged-in user for this request");
        }
        return toActor(jwt);
    }

    static Actor toActor(Jwt jwt) {
        List<String> roles = Optional.ofNullable(jwt.getClaimAsStringList("roles")).orElse(List.of());
        Set<ActorRole> actorRoles = roles.stream()
                .map(JwtCurrentActor::toRole)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return new Actor(new UserId(Long.parseLong(jwt.getSubject())), "ACTIVE".equals(jwt.getClaimAsString("status")),
                actorRoles);
    }

    /** "TOWER_ADMIN:1:7" -> tower admin of tower 7 in society 1; unknown roles are ignored. */
    private static ActorRole toRole(String claim) {
        String[] parts = claim.split(":");
        if (parts.length < 2 || Arrays.stream(ActorRole.Part.values()).noneMatch(p -> p.name().equals(parts[0]))) {
            return null;
        }
        TowerId tower = parts.length > 2 ? new TowerId(Long.parseLong(parts[2])) : null;
        return new ActorRole(ActorRole.Part.valueOf(parts[0]), new SocietyId(Long.parseLong(parts[1])), tower);
    }
}
