package com.example.schedulebook.domain.chat.dto.request;

public record ChatMessageSendRequest(
        Long roomId,
        String content,
        Long replyMessageId
) {
}
