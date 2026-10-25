package com.mycity.runwal.identity.application.query;

/** Everyone with this role in the society, e.g. the staff a complaint can be assigned to. */
public record UsersWithRoleQuery(long societyId, String role) {
}
