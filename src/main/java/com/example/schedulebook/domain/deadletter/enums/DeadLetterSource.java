package com.example.schedulebook.domain.deadletter.enums;

public enum DeadLetterSource {
    FORCE_LOGOUT_HANDLER,
    FORCE_LOGOUT_RETRY_SCHEDULER,
    REDIS_SUBSCRIBER,
    NOTIFICATION_RETRY_SCHEDULER,
    OUTBOX_SCHEDULER
}
