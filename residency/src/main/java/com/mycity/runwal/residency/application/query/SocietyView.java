package com.mycity.runwal.residency.application.query;

import com.mycity.runwal.residency.domain.model.society.Society;

public record SocietyView(long id, String name) {

    public static SocietyView of(Society society) {
        return new SocietyView(society.id().value(), society.name());
    }
}
