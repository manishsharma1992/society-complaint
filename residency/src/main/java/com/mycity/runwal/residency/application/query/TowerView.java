package com.mycity.runwal.residency.application.query;

import com.mycity.runwal.residency.domain.model.tower.Tower;

public record TowerView(long id, String code, String name, int residentialFloors) {

    public static TowerView of(Tower tower) {
        return new TowerView(tower.id().value(), tower.code().value(), tower.name(), tower.residentialFloors());
    }
}
