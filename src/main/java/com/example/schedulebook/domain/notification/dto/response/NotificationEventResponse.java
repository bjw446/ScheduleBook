package com.example.schedulebook.domain.notification.dto.response;

import com.example.schedulebook.domain.notification.enums.NotificationEventType;

public record NotificationEventResponse(
        NotificationEventType eventType,
        Long receiverId,
        Long notificationId,
        String type,
        String title,
        String message,
        long unreadCount
) {
}
