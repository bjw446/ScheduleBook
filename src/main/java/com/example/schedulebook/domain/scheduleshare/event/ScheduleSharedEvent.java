package com.example.schedulebook.domain.scheduleshare.event;

import com.example.schedulebook.domain.notification.event.NotificationEventMarker;

public record ScheduleSharedEvent(
        String eventId,
        Long receiverId,
        String ownerNickname,
        Long shareId
) implements NotificationEventMarker {
}
