package com.example.schedulebook.domain.schedule.event;

public record ScheduleCanceledEvent(
        String eventId,
        Long scheduleId,
        Long sharedUserId
) {
}
