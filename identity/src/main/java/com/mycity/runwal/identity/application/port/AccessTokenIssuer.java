package com.mycity.runwal.identity.application.port;

import com.mycity.runwal.identity.domain.model.user.AppUser;
import java.time.Instant;

/** Signs the login token the app sends with every request: who the user is, their status and roles. */
public interface AccessTokenIssuer {

    String issue(AppUser user, Instant issuedAt, Instant expiresAt);
}
