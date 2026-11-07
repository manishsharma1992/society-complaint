package com.mycity.runwal.residency.application.query;

import com.mycity.runwal.residency.domain.model.society.AddOn;
import com.mycity.runwal.residency.domain.model.society.Society;
import java.util.Arrays;
import java.util.List;

/**
 * @param latitude null while the society's location is unknown
 * @param addOns    every add-on, with whether it is available to the society and switched on
 */
public record SocietyView(long id, String name, Double latitude, Double longitude, List<AddOnView> addOns) {

    public static SocietyView of(Society society) {
        return new SocietyView(society.id().value(), society.name(),
                society.location().map(l -> l.latitude()).orElse(null),
                society.location().map(l -> l.longitude()).orElse(null),
                Arrays.stream(AddOn.values())
                        .map(a -> new AddOnView(a.name(), society.isAvailable(a), society.isOn(a)))
                        .toList());
    }
}
