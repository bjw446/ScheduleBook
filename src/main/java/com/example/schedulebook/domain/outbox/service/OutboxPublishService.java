package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.domain.outbox.event.OutboxSaveEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxPublishService {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(OutboxSaveEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
