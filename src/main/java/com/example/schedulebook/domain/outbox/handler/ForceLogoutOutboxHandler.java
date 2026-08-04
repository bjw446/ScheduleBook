package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.common.redis.publisher.RedisEventPublisher;
import com.example.schedulebook.domain.auth.event.ForceLogoutSessionEvent;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ForceLogoutOutboxHandler implements OutboxEventHandler<ForceLogoutSessionEvent> {
    private final RedisEventPublisher redisEventPublisher;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.FORCE_LOGOUT;
    }

    @Override
    public Class<ForceLogoutSessionEvent> payloadType() {
        return ForceLogoutSessionEvent.class;
    }

    @Override
    public void handle(Long outboxId, ForceLogoutSessionEvent payload) {
        redisEventPublisher.publish(payload);
    }
}
