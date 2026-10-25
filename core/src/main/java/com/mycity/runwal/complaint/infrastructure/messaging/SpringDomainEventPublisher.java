package com.mycity.runwal.complaint.infrastructure.messaging;

import com.mycity.runwal.complaint.application.port.DomainEventPublisher;
import com.mycity.runwal.shared.ddd.DomainEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes domain events inside this service. A transactional outbox can replace this when other services listen. */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringDomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
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
