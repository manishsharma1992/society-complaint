package com.mycity.runwal.residency.domain.model.society;

import com.mycity.runwal.residency.domain.exception.InvalidResidencyException;
import com.mycity.runwal.shared.ddd.annotation.AggregateRoot;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A housing complex, e.g. "Runwal MyCity". Everything in the app belongs to exactly one society.
 * Which add-ons it uses is decided twice: the platform owner makes an add-on available to the society, then the
 * society chooses whether to switch it on. Withdrawing an add-on also switches it off.
 */
@AggregateRoot
public class Society {

    public static final int MAX_NAME_LENGTH = 150;

    private final SocietyId id;
    private final String name;
    private final GeoPoint location;
    private final Set<AddOn> available;
    private final Set<AddOn> enabled;

    public Society(SocietyId id, String name) {
        this(id, name, null, Set.of(), Set.of());
    }

    /** @param location null while unknown (the society is then nobody's neighbour) */
    public Society(SocietyId id, String name, GeoPoint location, Set<AddOn> available, Set<AddOn> enabled) {
        this.id = Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH) {
            throw new InvalidResidencyException("A society needs a name of at most " + MAX_NAME_LENGTH + " characters");
        }
        this.name = name.strip();
        this.location = location;
        this.available = copy(available);
        this.enabled = copy(enabled);
        if (!this.available.containsAll(this.enabled)) {
            throw new InvalidResidencyException("Only an available add-on can be switched on");
        }
    }

    /** The platform owner lets this society use the add-on (it stays off until the society switches it on). */
    public void makeAvailable(AddOn addOn) {
        available.add(Objects.requireNonNull(addOn, "addOn"));
    }

    /** The platform owner takes the add-on away; it is switched off too. Its data stays for a later return. */
    public void withdraw(AddOn addOn) {
        available.remove(addOn);
        enabled.remove(addOn);
    }

    /** The society starts using an add-on the platform made available to it. */
    public void switchOn(AddOn addOn) {
        if (!available.contains(addOn)) {
            throw new InvalidResidencyException(addOn + " is not available to this society");
        }
        enabled.add(addOn);
    }

    public void switchOff(AddOn addOn) {
        enabled.remove(addOn);
    }

    public boolean isAvailable(AddOn addOn) {
        return available.contains(addOn);
    }

    public boolean isOn(AddOn addOn) {
        return enabled.contains(addOn);
    }

    /** Within {@code meters} of the other society (itself included); false when either location is unknown. */
    public boolean isNear(Society other, double meters) {
        return location != null && other.location != null && location.metersTo(other.location) <= meters;
    }

    private static Set<AddOn> copy(Set<AddOn> addOns) {
        return addOns.isEmpty() ? EnumSet.noneOf(AddOn.class) : EnumSet.copyOf(addOns);
    }

    public SocietyId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Optional<GeoPoint> location() {
        return Optional.ofNullable(location);
    }

    public Set<AddOn> availableAddOns() {
        return Set.copyOf(available);
    }

    public Set<AddOn> enabledAddOns() {
        return Set.copyOf(enabled);
    }
}
