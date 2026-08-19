package com.example.schedulebook.domain.schedule.event;

public record ScheduleDeletedEvent(
        String eventId,
        Long scheduleId
) {
}
