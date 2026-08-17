package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.redis.publisher.RedisEventPublisher;
import com.example.schedulebook.domain.notification.dto.response.NotificationEventResponse;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationOutboxHandler implements OutboxEventHandler<NotificationEventResponse> {
    private final RedisEventPublisher redisEventPublisher;

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
        redisEventPublisher.publish(RedisConst.NOTIFICATION, payload);
    }
}
