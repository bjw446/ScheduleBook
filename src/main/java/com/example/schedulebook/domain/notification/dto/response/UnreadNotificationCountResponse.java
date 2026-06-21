package com.example.schedulebook.domain.notification.dto.response;

public record UnreadNotificationCountResponse(
        long count
) {
    public static UnreadNotificationCountResponse from(long count) {
        return new UnreadNotificationCountResponse(count);
    }
}
