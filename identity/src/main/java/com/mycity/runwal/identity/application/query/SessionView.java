package com.mycity.runwal.identity.application.query;

import java.time.Instant;

/** A logged-in user: send {@code token} as "Authorization: Bearer <token>" until it expires. */
public record SessionView(String token, Instant expiresAt, UserView user) {
}
