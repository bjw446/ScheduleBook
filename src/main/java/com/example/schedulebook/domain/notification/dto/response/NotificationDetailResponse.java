package com.example.schedulebook.domain.notification.dto.response;

import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationDetailResponse(
        Long notificationId,
        String title,
        String content,
        NotificationType notificationType,
        boolean isRead,
        Long targetId,
        LocalDateTime createdAt
) {
    public static NotificationDetailResponse from(Notification notification) {
        return new NotificationDetailResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getNotificationType(),
                notification.isRead(),
                notification.getTargetId(),
                notification.getCreatedAt()
        );
    }
}
