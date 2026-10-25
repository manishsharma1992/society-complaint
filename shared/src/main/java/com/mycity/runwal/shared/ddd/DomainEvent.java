package com.mycity.runwal.shared.ddd;

import java.time.Instant;

/**
 * Something that happened in the domain that domain experts care about.
 * Implementations are immutable records named in the past tense, e.g. {@code ComplaintRaised}.
 */
public interface DomainEvent {

    /** When the event happened. */
    Instant occurredOn();
}
