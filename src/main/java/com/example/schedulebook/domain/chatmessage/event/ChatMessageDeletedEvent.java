package com.example.schedulebook.domain.chatmessage.event;

public record ChatMessageDeletedEvent(
        Long roomId,
        Long messageId,
        String content
) {
}
