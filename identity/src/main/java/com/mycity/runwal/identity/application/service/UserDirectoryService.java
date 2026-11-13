package com.mycity.runwal.identity.application.service;

import com.mycity.runwal.identity.application.port.CurrentUser;
import com.mycity.runwal.identity.application.query.UserNameView;
import com.mycity.runwal.identity.application.query.UserNamesQuery;
import com.mycity.runwal.identity.application.query.UsersWithRoleQuery;
import com.mycity.runwal.identity.application.query.StaffOfSocietyQuery;
import com.mycity.runwal.identity.application.query.StaffMemberView;
import com.mycity.runwal.identity.domain.exception.InvalidIdentityException;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.AppUserRepository;
import com.mycity.runwal.identity.domain.model.user.Role;
import com.mycity.runwal.identity.domain.model.user.RoleAssignment;
import com.mycity.runwal.identity.domain.model.user.UserStatus;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.UserId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who is who in a society, for showing names next to complaints and comments, and for picking staff.
 * Only names are shared, never phone numbers, and only of people with a role in that society.
 */
@ApplicationService
public class UserDirectoryService {

    private final AppUserRepository users;
    private final CurrentUser currentUser;

    public UserDirectoryService(AppUserRepository users, CurrentUser currentUser) {
        this.users = users;
        this.currentUser = currentUser;
    }

    /** Any active member of the society may look up names. */
    @Transactional(readOnly = true)
    public List<UserNameView> names(UserNamesQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        requireCaller(societyId, "look up names", Role.values());
        if (query.userIds().size() > UserNamesQuery.MAX_IDS) {
            throw new InvalidIdentityException("Ask for at most " + UserNamesQuery.MAX_IDS + " names at a time");
        }
        List<UserId> ids = query.userIds().stream().distinct().map(UserId::new).toList();
        return users.findAllById(ids).stream()
                .filter(user -> user.roles().stream().anyMatch(r -> r.societyId().equals(societyId)))
                .map(UserNameView::of)
                .toList();
    }

    /**
     * For other contexts (no caller check): approved staff and the towers they work in, then the tower managers
     * who are not also staff. Ids only, no names.
     */
    @Transactional(readOnly = true)
    public List<StaffMemberView> staffOf(StaffOfSocietyQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        List<StaffMemberView> staff = active(Role.STAFF, societyId);
        Set<Long> staffIds = staff.stream().map(StaffMemberView::userId).collect(Collectors.toSet());
        return Stream.concat(staff.stream(),
                active(Role.TOWER_ADMIN, societyId).stream().filter(m -> !staffIds.contains(m.userId()))).toList();
    }

    private List<StaffMemberView> active(Role role, SocietyId societyId) {
        return users.findWithRole(role, societyId).stream()
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .map(user -> {
                    List<RoleAssignment> roles = user.roles().stream()
                            .filter(r -> r.role() == role && r.societyId().equals(societyId)).toList();
                    boolean everywhere = roles.stream().anyMatch(r -> r.towerId() == null);
                    return new StaffMemberView(user.id().value(), everywhere ? List.of()
                            : roles.stream().map(r -> r.towerId().value()).distinct().sorted().toList(),
                            role == Role.TOWER_ADMIN);
                })
                .toList();
    }

    /** Admins list e.g. the staff they can assign complaints to. */
    @Transactional(readOnly = true)
    public List<UserNameView> withRole(UsersWithRoleQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        Role role = Role.from(query.role());
        requireCaller(societyId, "list users by role", Role.SOCIETY_ADMIN, Role.TOWER_ADMIN);
        return users.findWithRole(role, societyId).stream().map(UserNameView::of).toList();
    }

    private void requireCaller(SocietyId societyId, String action, Role... anyOf) {
        boolean allowed = users.findById(currentUser.userId())
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .map(user -> Arrays.stream(anyOf).anyMatch(role -> user.hasRoleIn(role, societyId)))
                .orElse(false);
        if (!allowed) {
            throw new ActionNotAllowedException("You may not " + action + " in this society");
        }
    }
}
