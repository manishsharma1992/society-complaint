package com.mycity.runwal.identity.interfaces.rest;

import com.mycity.runwal.identity.application.query.UserNameView;
import com.mycity.runwal.identity.application.query.UserNamesQuery;
import com.mycity.runwal.identity.application.query.UsersWithRoleQuery;
import com.mycity.runwal.identity.application.service.UserDirectoryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Names of the society's people: GET .../users?ids=4,3 (names for complaints and comments)
 * or GET .../users?role=STAFF (whom to assign). Never returns phone numbers.
 */
@RestController
@RequestMapping("/societies/{societyId}/users")
class UserDirectoryController {

    private final UserDirectoryService directory;

    UserDirectoryController(UserDirectoryService directory) {
        this.directory = directory;
    }

    @GetMapping
    ResponseEntity<List<UserNameResponse>> users(@PathVariable long societyId,
                                                 @RequestParam(required = false) List<Long> ids,
                                                 @RequestParam(required = false) String role) {
        if ((ids == null) == (role == null)) {
            return ResponseEntity.badRequest().build();
        }
        List<UserNameView> found = ids != null
                ? directory.names(new UserNamesQuery(societyId, ids))
                : directory.withRole(new UsersWithRoleQuery(societyId, role));
        return ResponseEntity.ok(found.stream().map(UserNameResponse::from).toList());
    }

    record UserNameResponse(long id, String name) {

        static UserNameResponse from(UserNameView view) {
            return new UserNameResponse(view.id(), view.name());
        }
    }
}
