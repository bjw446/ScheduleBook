package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.domain.notification.event.NotificationEventMarker;

import java.time.LocalDateTime;

public record ScheduleReminderEvent(
        String eventId,
        Long scheduleId,
        Long receiverId,
        String title,
        LocalDateTime reminderTime
) implements NotificationEventMarker {
}
