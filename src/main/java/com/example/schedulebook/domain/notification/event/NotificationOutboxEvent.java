package com.example.schedulebook.domain.notification.event;

public record NotificationOutboxEvent(
        Long outboxId,
        NotificationEventMarker payload
) {
}
