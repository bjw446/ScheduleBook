package com.example.schedulebook.domain.chatmessage.dto.request;

public record ChatMessageScheduleShareRequest(
        Long roomId,
        Long scheduleId
) {
}
