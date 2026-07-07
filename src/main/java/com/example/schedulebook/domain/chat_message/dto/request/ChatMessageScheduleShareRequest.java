package com.example.schedulebook.domain.chat_message.dto.request;

public record ChatMessageScheduleShareRequest(
        Long roomId,
        Long scheduleId
) {
}
