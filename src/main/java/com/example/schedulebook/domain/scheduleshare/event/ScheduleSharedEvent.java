package com.example.schedulebook.domain.scheduleshare.event;

public record ScheduleSharedEvent(
        Long receiverId,
        String ownerNickname,
        Long shareId
) {
}
