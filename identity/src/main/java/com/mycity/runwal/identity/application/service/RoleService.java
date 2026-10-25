package com.mycity.runwal.identity.application.service;

import com.mycity.runwal.identity.application.command.GrantRoleCommand;
import com.mycity.runwal.identity.application.command.RevokeRoleCommand;
import com.mycity.runwal.identity.application.port.CurrentUser;
import com.mycity.runwal.identity.application.port.DomainEventPublisher;
import com.mycity.runwal.identity.application.query.UserView;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.AppUserRepository;
import com.mycity.runwal.identity.domain.model.user.Role;
import com.mycity.runwal.identity.domain.model.user.RoleAssignment;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Clock;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * The society admin gives and takes roles (staff, tower admin, society admin). Returns empty when there is no
 * such user. Users with an existing login keep their old roles in the token until they log in again.
 */
@ApplicationService
public class RoleService {

    private final AppUserRepository users;
    private final SocietyAdminCheck societyAdmin;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public RoleService(AppUserRepository users, CurrentUser currentUser, DomainEventPublisher eventPublisher,
                       Clock clock) {
        this.users = users;
        this.societyAdmin = new SocietyAdminCheck(users, currentUser);
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public Optional<UserView> grant(GrantRoleCommand command) {
        AppUser admin = societyAdmin.require(new SocietyId(command.societyId()), "grant roles");
        RoleAssignment role = assignment(command.role(), command.societyId(), command.towerId());
        return users.findById(new UserId(command.userId())).map(user -> {
            user.grantRole(role, admin.id(), clock.instant());
            return saveAndPublish(user);
        });
    }

    @Transactional
    public Optional<UserView> revoke(RevokeRoleCommand command) {
        AppUser admin = societyAdmin.require(new SocietyId(command.societyId()), "revoke roles");
        RoleAssignment role = assignment(command.role(), command.societyId(), command.towerId());
        return users.findById(new UserId(command.userId())).map(user -> {
            user.revokeRole(role, admin.id(), clock.instant());
            return saveAndPublish(user);
        });
    }

    private static RoleAssignment assignment(String role, long societyId, Long towerId) {
        return new RoleAssignment(Role.from(role), new SocietyId(societyId),
                towerId == null ? null : new TowerId(towerId));
    }

    private UserView saveAndPublish(AppUser user) {
        users.save(user);
        eventPublisher.publish(user.pullDomainEvents());
        return UserView.of(user);
    }
}
