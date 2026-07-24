package com.example.schedulebook.domain.outbox.publisher;

import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(Outbox outbox) throws Exception {
        Class<?> eventClass = outbox.getEventType().getEventClass();

        Object event = objectMapper.readValue(
                outbox.getPayload(),
                eventClass
        );

        applicationEventPublisher.publishEvent(event);
    }
}
