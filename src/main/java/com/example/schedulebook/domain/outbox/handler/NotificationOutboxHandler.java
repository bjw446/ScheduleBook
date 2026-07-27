package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.notification.publisher.NotificationEventPublisher;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationOutboxHandler implements OutboxEventHandler<NotificationEventResponse> {
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.NOTIFICATION_EVENT;
    }

    @Override
    public Class<NotificationEventResponse> payloadType() {
        return NotificationEventResponse.class;
    }

    @Override
    public void handle(Long outboxId, NotificationEventResponse payload) {
        notificationEventPublisher.publish(payload);
    }
}
