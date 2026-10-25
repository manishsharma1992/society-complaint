package com.mycity.runwal.residency.interfaces.rest;

import com.mycity.runwal.residency.application.query.FlatMemberView;
import com.mycity.runwal.residency.application.query.FlatView;
import java.time.Instant;
import java.util.List;

/** @param label e.g. "B2-503" */
record FlatResponse(long id, long towerId, String towerCode, int floor, int number, String label,
                    List<MemberResponse> members) {

    static FlatResponse from(FlatView view) {
        return new FlatResponse(view.id(), view.towerId(), view.towerCode(), view.floor(), view.number(),
                view.label(), view.members().stream().map(MemberResponse::from).toList());
    }

    record MemberResponse(long userId, String type, Instant since) {

        static MemberResponse from(FlatMemberView view) {
            return new MemberResponse(view.userId(), view.type(), view.since());
        }
    }
}
