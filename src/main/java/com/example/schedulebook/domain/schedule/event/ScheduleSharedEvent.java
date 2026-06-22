package com.example.schedulebook.domain.schedule.event;

public record ScheduleSharedEvent(
        Long receiverId,
        String ownerNickname,
        Long shareId
) {
}
