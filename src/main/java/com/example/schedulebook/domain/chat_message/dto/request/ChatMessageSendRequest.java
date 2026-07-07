package com.example.schedulebook.domain.chat_message.dto.request;

public record ChatMessageSendRequest(
        Long roomId,
        String content,
        Long replyMessageId
) {
}
