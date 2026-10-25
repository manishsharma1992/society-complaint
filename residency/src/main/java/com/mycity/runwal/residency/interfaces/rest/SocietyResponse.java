package com.mycity.runwal.residency.interfaces.rest;

import com.mycity.runwal.residency.application.query.SocietyView;

record SocietyResponse(long id, String name) {

    static SocietyResponse from(SocietyView view) {
        return new SocietyResponse(view.id(), view.name());
    }
}
