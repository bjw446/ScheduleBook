package com.example.schedulebook.domain.deadletter.enums;

public enum DeadLetterSource {
    FORCE_LOGOUT_RETRY_SCHEDULER,
    OUTBOX_TRANSACTION_SERVICE,
    OUTBOX_SERVICE
}
