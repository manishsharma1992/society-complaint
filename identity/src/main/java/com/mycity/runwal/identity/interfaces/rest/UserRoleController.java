package com.mycity.runwal.identity.interfaces.rest;

import com.mycity.runwal.identity.application.command.GrantRoleCommand;
import com.mycity.runwal.identity.application.command.RevokeRoleCommand;
import com.mycity.runwal.identity.application.service.RoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gives and takes roles in a society: STAFF, TOWER_ADMIN (one tower), SOCIETY_ADMIN, RESIDENT. */
@RestController
@RequestMapping("/societies/{societyId}/users/{userId}/roles")
class UserRoleController {

    private final RoleService roleService;

    UserRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    ResponseEntity<UserResponse> grant(@PathVariable long societyId, @PathVariable long userId,
                                       @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.of(roleService.grant(
                        new GrantRoleCommand(societyId, userId, request.role(), request.towerId()))
                .map(UserResponse::from));
    }

    /** e.g. DELETE .../roles?role=TOWER_ADMIN&towerId=7 */
    @DeleteMapping
    ResponseEntity<UserResponse> revoke(@PathVariable long societyId, @PathVariable long userId,
                                        @RequestParam String role, @RequestParam(required = false) Long towerId) {
        return ResponseEntity.of(roleService.revoke(new RevokeRoleCommand(societyId, userId, role, towerId))
                .map(UserResponse::from));
    }

    record RoleRequest(@NotNull @Pattern(regexp = "RESIDENT|STAFF|TOWER_ADMIN|SOCIETY_ADMIN") String role,
                       @Positive Long towerId) {
    }
}
