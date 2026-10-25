package com.mycity.runwal.shared.ddd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class for aggregate roots that records the domain events raised by their behaviour.
 * The application service saves the aggregate, then pulls and publishes the recorded events.
 */
public abstract class AbstractAggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected final void recordEvent(DomainEvent event) {
        domainEvents.add(Objects.requireNonNull(event, "event"));
    }

    /** Returns the events recorded since the last call and forgets them. */
    public final List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}
