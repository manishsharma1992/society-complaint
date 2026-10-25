package com.mycity.runwal.identity.application.query;

import com.mycity.runwal.identity.domain.model.user.AppUser;

/** Only what other people may see of a user: no phone number (India DPDP Act). */
public record UserNameView(long id, String name) {

    public static UserNameView of(AppUser user) {
        return new UserNameView(user.id().value(), user.name().orElse("User " + user.id().value()));
    }
}
