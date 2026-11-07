package com.mycity.runwal.feed.infrastructure.messaging;

import com.mycity.runwal.feed.application.port.DomainEventPublisher;
import com.mycity.runwal.shared.ddd.DomainEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events inside the app. Feed events never carry the author, so logging them is safe.
 * A transactional outbox can replace this when other services listen.
 */
@Component
class FeedEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FeedEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    FeedEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
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
