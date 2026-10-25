package com.mycity.runwal.residency.infrastructure.messaging;

import com.mycity.runwal.residency.application.port.DomainEventPublisher;
import com.mycity.runwal.shared.ddd.DomainEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes domain events inside the app. A transactional outbox can replace this when other services listen. */
@Component
class ResidencyEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ResidencyEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    ResidencyEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            log.info("Domain event {}", event);
            applicationEventPublisher.publishEvent(event);
        }
    }
}
