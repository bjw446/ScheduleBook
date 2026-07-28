package com.example.schedulebook.domain.outbox.publisher;

import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(Long outboxId, Outbox outbox) throws Exception {
        try {
            Class<?> eventClass = outbox.getEventType().getEventClass();

            Object event = objectMapper.readValue(
                    outbox.getPayload(),
                    eventClass
            );

            applicationEventPublisher.publishEvent(event);

        } catch (Exception e) {
            log.error("Outbox Publish 실패 {}", outboxId);

            throw e;
        }
    }
}
