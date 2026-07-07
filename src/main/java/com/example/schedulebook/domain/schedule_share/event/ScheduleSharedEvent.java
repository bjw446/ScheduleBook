package com.example.schedulebook.domain.schedule_share.event;

public record ScheduleSharedEvent(
        Long receiverId,
        String ownerNickname,
        Long shareId
) {
}
