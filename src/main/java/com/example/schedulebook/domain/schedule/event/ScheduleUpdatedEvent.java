package com.example.schedulebook.domain.schedule.event;

public record ScheduleUpdatedEvent(
        String eventId,
        Long scheduleId
) {
}
