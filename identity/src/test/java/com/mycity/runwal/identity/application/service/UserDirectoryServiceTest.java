package com.mycity.runwal.identity.application.service;

import com.mycity.runwal.identity.application.query.StaffOfSocietyQuery;
import com.mycity.runwal.identity.application.query.StaffMemberView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycity.runwal.identity.application.query.UserNameView;
import com.mycity.runwal.identity.application.query.UserNamesQuery;
import com.mycity.runwal.identity.application.query.UsersWithRoleQuery;
import com.mycity.runwal.identity.domain.exception.InvalidIdentityException;
import com.mycity.runwal.identity.domain.model.user.AppUser;
import com.mycity.runwal.identity.domain.model.user.PhoneNumber;
import com.mycity.runwal.identity.domain.model.user.Role;
import com.mycity.runwal.identity.domain.model.user.RoleAssignment;
import com.mycity.runwal.identity.domain.model.user.UserStatus;
import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.shared.id.TowerId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class UserDirectoryServiceTest {

    private static final SocietyId RUNWAL = new SocietyId(1);

    private final IdentityFakes.Users users = new IdentityFakes.Users();
    private long caller = 4;
    private final UserDirectoryService directory = new UserDirectoryService(users, () -> new UserId(caller));

    UserDirectoryServiceTest() {
        save(1, "Asha (Admin)", UserStatus.ACTIVE, RoleAssignment.societyWide(Role.SOCIETY_ADMIN, RUNWAL));
        save(2, "Ravi (B2 Admin)", UserStatus.ACTIVE, new RoleAssignment(Role.TOWER_ADMIN, RUNWAL, new TowerId(7)));
        save(3, "Suresh (Plumber)", UserStatus.ACTIVE, RoleAssignment.societyWide(Role.STAFF, RUNWAL));
        save(5, "Anil (Electrician)", UserStatus.ACTIVE, new RoleAssignment(Role.STAFF, RUNWAL, new TowerId(7)));
        save(4, "Meera (B2-503)", UserStatus.ACTIVE, RoleAssignment.societyWide(Role.RESIDENT, RUNWAL));
        save(6, "Other society", UserStatus.ACTIVE, RoleAssignment.societyWide(Role.RESIDENT, new SocietyId(2)));
        save(7, null, UserStatus.VERIFIED, null);
    }

    @Test
    void membersLookUpNamesOfTheirSocietyOnly() {
        List<UserNameView> names = directory.names(new UserNamesQuery(1, List.of(3L, 1L, 3L, 6L, 7L, 99L)));

        assertThat(names).containsExactlyInAnyOrder(new UserNameView(3, "Suresh (Plumber)"),
                new UserNameView(1, "Asha (Admin)"));
    }

    @Test
    void notYetApprovedUsersCannotLookUpNames() {
        caller = 7;

        assertThatThrownBy(() -> directory.names(new UserNamesQuery(1, List.of(1L))))
                .isInstanceOf(ActionNotAllowedException.class);
        caller = 6;
        assertThatThrownBy(() -> directory.names(new UserNamesQuery(1, List.of(1L))))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    @Test
    void atMostHundredIdsAtATime() {
        List<Long> ids = LongStream.rangeClosed(1, 101).boxed().toList();

        assertThatThrownBy(() -> directory.names(new UserNamesQuery(1, ids)))
                .isInstanceOf(InvalidIdentityException.class);
    }

    @Test
    void adminsListStaffByName() {
        caller = 2;

        assertThat(directory.withRole(new UsersWithRoleQuery(1, "STAFF"))).extracting(UserNameView::name)
                .containsExactly("Anil (Electrician)", "Suresh (Plumber)");
    }

    @Test
    void residentsCannotListUsersByRole() {
        assertThatThrownBy(() -> directory.withRole(new UsersWithRoleQuery(1, "STAFF")))
                .isInstanceOf(ActionNotAllowedException.class);
    }

    private void save(long id, String name, UserStatus status, RoleAssignment role) {
        users.save(new AppUser(new UserId(id), PhoneNumber.parse("98765432" + String.format("%02d", id)),
                Instant.parse("2026-10-25T08:00:00Z"), name, status, null, null,
                role == null ? List.of() : List.of(role)));
    }

    @Test
    void otherContextsGetTheApprovedStaffAndWhereTheyWork() {
        assertThat(directory.staffOf(new StaffOfSocietyQuery(1))).containsExactlyInAnyOrder(
                new StaffMemberView(3, List.of()), new StaffMemberView(5, List.of(7L)));
    }
}
