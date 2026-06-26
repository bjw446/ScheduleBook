package com.example.schedulebook.domain.chat.dto.request;

public record ChatMessageScheduleShareRequest(
        Long roomId,
        Long scheduleId
) {
}
