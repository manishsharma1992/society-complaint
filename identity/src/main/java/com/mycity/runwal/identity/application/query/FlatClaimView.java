package com.mycity.runwal.identity.application.query;

import com.mycity.runwal.identity.domain.model.user.FlatClaim;

public record FlatClaimView(long societyId, long flatId, String occupancy) {

    static FlatClaimView of(FlatClaim claim) {
        return new FlatClaimView(claim.societyId().value(), claim.flatId().value(), claim.occupancy().name());
    }
}
