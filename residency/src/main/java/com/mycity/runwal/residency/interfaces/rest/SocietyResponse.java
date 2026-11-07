package com.mycity.runwal.residency.interfaces.rest;

import com.mycity.runwal.residency.application.query.AddOnView;
import com.mycity.runwal.residency.application.query.SocietyView;
import java.util.List;

/** @param latitude null while the society's location is unknown */
record SocietyResponse(long id, String name, Double latitude, Double longitude, List<AddOnResponse> addOns) {

    static SocietyResponse from(SocietyView view) {
        return new SocietyResponse(view.id(), view.name(), view.latitude(), view.longitude(),
                view.addOns().stream().map(AddOnResponse::from).toList());
    }

    record AddOnResponse(String addOn, boolean available, boolean on) {

        static AddOnResponse from(AddOnView view) {
            return new AddOnResponse(view.addOn(), view.available(), view.on());
        }
    }
}
