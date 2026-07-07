package com.example.schedulebook.domain.chat_message.event;

public record ChatMessageDeletedEvent(
        Long roomId,
        Long messageId,
        String content
) {
}
