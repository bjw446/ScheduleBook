package com.example.schedulebook.domain.outbox.dto;

import com.example.schedulebook.domain.outbox.enums.OutboxStatus;

import java.time.LocalDateTime;

public record RetryDecision(
        OutboxStatus outboxStatus,
        LocalDateTime nextRetryAt
) {
    public boolean isDead() {
        return outboxStatus == OutboxStatus.DEAD;
    }
}
