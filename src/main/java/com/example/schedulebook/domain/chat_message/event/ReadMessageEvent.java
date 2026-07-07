package com.example.schedulebook.domain.chat_message.event;

import java.time.LocalDateTime;

public record ReadMessageEvent(
        Long roomId,
        Long userId,
        Long lastReadMessageId,
        LocalDateTime readAt
) {
    public static ReadMessageEvent from(Long roomId, Long userId, Long lastReadMessageId) {
        return new ReadMessageEvent(
                roomId,
                userId,
                lastReadMessageId,
                LocalDateTime.now()
        );
    }
}
