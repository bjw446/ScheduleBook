package com.example.schedulebook.domain.admin.dto.response;

import com.example.schedulebook.domain.outbox.entity.Outbox;
import com.example.schedulebook.domain.outbox.enums.OutboxAggregateType;
import com.example.schedulebook.domain.outbox.enums.OutboxEventType;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;

import java.time.LocalDateTime;

public record OutboxResponse(
        Long id,
        OutboxAggregateType aggregateType,
        Long aggregateId,
        OutboxEventType eventType,
        OutboxStatus status,
        String payloadPreview,
        int retryCount,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OutboxResponse from(Outbox outbox) {
        String preview = outbox.getPayload() == null
                ? null
                : outbox.getPayload().length() > 200
                ? outbox.getPayload().substring(0, 200)
                : outbox.getPayload();

        return new OutboxResponse(
                outbox.getId(),
                outbox.getAggregateType(),
                outbox.getAggregateId(),
                outbox.getEventType(),
                outbox.getStatus(),
                preview,
                outbox.getRetryCount(),
                outbox.getErrorMessage(),
                outbox.getCreatedAt(),
                outbox.getUpdatedAt()
        );
    }
}
