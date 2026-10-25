package com.mycity.runwal.identity.application.query;

import com.mycity.runwal.identity.domain.model.user.AppUser;
import java.util.List;

/**
 * Read model of a user, in plain types so the interfaces layer never sees the domain.
 *
 * @param name  null until the user registers
 * @param claim the flat the user registered for, or null
 */
public record UserView(long id, String phone, String name, String status, List<RoleView> roles, FlatClaimView claim,
                       String rejectionReason) {

    public static UserView of(AppUser user) {
        return new UserView(user.id().value(), user.phone().value(), user.name().orElse(null), user.status().name(),
                user.roles().stream().map(RoleView::of).toList(),
                user.claim().map(FlatClaimView::of).orElse(null), user.rejectionReason().orElse(null));
    }
}
