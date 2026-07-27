package com.example.schedulebook.domain.outbox.handler;

import com.example.schedulebook.domain.outbox.enums.OutboxEventType;

public interface OutboxEventHandler<T> {
    OutboxEventType supports();

    Class<T> payloadType();

    void handle(Long outboxId, T payload);
}
