package com.mycity.runwal.identity.infrastructure.security;

import com.mycity.runwal.identity.application.port.CurrentLogin;
import com.mycity.runwal.identity.application.port.CurrentUser;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** The user ID and the end of the login token the app verified for this request. */
@Component
class JwtCurrentUser implements CurrentUser, CurrentLogin {

    @Override
    public UserId userId() {
        return new UserId(Long.parseLong(token().getSubject()));
    }

    @Override
    public Instant endsAt() {
        return token().getExpiresAt();
    }

    private static Jwt token() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No logged-in user for this request");
        }
        return jwt;
    }
}
