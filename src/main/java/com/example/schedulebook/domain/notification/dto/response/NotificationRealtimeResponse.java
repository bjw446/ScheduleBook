package com.example.schedulebook.domain.notification.dto.response;

public record NotificationRealtimeResponse(
        Long receiverId,
        String type,
        String title,
        String message
) {
}
