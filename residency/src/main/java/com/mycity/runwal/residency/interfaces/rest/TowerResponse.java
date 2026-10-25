package com.mycity.runwal.residency.interfaces.rest;

import com.mycity.runwal.residency.application.query.TowerView;

record TowerResponse(long id, String code, String name, int residentialFloors) {

    static TowerResponse from(TowerView view) {
        return new TowerResponse(view.id(), view.code(), view.name(), view.residentialFloors());
    }
}
