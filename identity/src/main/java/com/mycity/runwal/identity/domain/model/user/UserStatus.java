package com.mycity.runwal.identity.domain.model.user;

/** Where a user is in self sign-up. Only ACTIVE users can use the app beyond registering. */
public enum UserStatus {
    /** Proved they own the phone number (OTP), has not registered yet. */
    VERIFIED,
    /** Registered for a flat, waiting for the society to approve. */
    PENDING,
    /** Approved resident, or given a role (staff, admin). */
    ACTIVE,
    /** Registration was rejected; may register again. */
    REJECTED
}
