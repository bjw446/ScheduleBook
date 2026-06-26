package com.example.schedulebook.domain.chat.event;

public record ChatMessageDeletedEvent(
        Long roomId,
        Long messageId,
        String content
) {
}
