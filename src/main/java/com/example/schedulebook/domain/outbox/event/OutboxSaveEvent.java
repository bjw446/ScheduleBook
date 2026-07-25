package com.example.schedulebook.domain.outbox.event;

import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;

public record OutboxSaveEvent(
        OutboxAggregateType aggregateType,
        Long aggregateId,
        OutboxEventType eventType,
        Object payload
) {
}
