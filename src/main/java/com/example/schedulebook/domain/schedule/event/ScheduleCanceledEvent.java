package com.example.schedulebook.domain.schedule.event;

public record ScheduleCanceledEvent(
        Long scheduleId,
        Long sharedUserId
) {
}
