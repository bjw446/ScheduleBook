package com.example.schedulebook.domain.notification.dto.response;

import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationSummaryResponse(
        Long notificationId,
        String title,
        NotificationType notificationType,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationSummaryResponse from(Notification notification) {
        return new NotificationSummaryResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getNotificationType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
