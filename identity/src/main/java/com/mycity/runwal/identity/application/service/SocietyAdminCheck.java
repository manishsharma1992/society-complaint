package com.mycity.runwal.identity.application.service;

import com.mycity.runwal.identity.application.port.CurrentUser;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.AppUserRepository;
import com.mycity.runwal.identity.domain.model.user.Role;
import com.mycity.runwal.identity.domain.model.user.UserStatus;
import com.mycity.runwal.shared.id.SocietyId;

/**
 * Only an active society admin reviews sign-ups and grants roles. Checked against the stored user, not the token,
 * so taking the role away takes effect immediately.
 */
final class SocietyAdminCheck {

    private final AppUserRepository users;
    private final CurrentUser currentUser;

    SocietyAdminCheck(AppUserRepository users, CurrentUser currentUser) {
        this.users = users;
        this.currentUser = currentUser;
    }

    AppUser require(SocietyId societyId, String action) {
        return users.findById(currentUser.userId())
                .filter(user -> user.status() == UserStatus.ACTIVE && user.hasRoleIn(Role.SOCIETY_ADMIN, societyId))
                .orElseThrow(() -> new ActionNotAllowedException("Only the society admin can " + action));
    }
}
