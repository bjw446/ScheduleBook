package com.example.schedulebook.domain.deadletter.enums;

public enum DeadLetterAggregateType {
    SESSION,
    OUTBOX,
    NOTIFICATION_RETRY,
    DESERIALIZATION_ERROR
}
