package com.mycity.runwal.workforce.application.port;

import com.mycity.runwal.shared.ddd.DomainEvent;
import java.util.List;

public interface DomainEventPublisher {

    void publish(List<DomainEvent> events);
}
