package com.example.schedulebook.domain.chat.dto.response;

public record ChatMessageDeletedEvent(
        Long roomId,
        Long messageId,
        String content
) {
}
