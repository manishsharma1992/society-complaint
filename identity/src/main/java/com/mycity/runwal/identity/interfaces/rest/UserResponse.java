package com.mycity.runwal.identity.interfaces.rest;

import com.mycity.runwal.identity.application.query.FlatClaimView;
import com.mycity.runwal.identity.application.query.RoleView;
import com.mycity.runwal.identity.application.query.UserView;
import java.util.List;

record UserResponse(long id, String phone, String name, String status, List<RoleResponse> roles,
                    ClaimResponse claim, String rejectionReason, boolean platformOwner) {

    static UserResponse from(UserView view) {
        return new UserResponse(view.id(), view.phone(), view.name(), view.status(),
                view.roles().stream().map(RoleResponse::from).toList(),
                view.claim() == null ? null : ClaimResponse.from(view.claim()), view.rejectionReason(),
                view.platformOwner());
    }

    record RoleResponse(String role, long societyId, Long towerId) {

        static RoleResponse from(RoleView view) {
            return new RoleResponse(view.role(), view.societyId(), view.towerId());
        }
    }

    record ClaimResponse(long societyId, long flatId, String occupancy) {

        static ClaimResponse from(FlatClaimView view) {
            return new ClaimResponse(view.societyId(), view.flatId(), view.occupancy());
        }
    }
}
