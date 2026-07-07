package com.example.schedulebook.domain.chatmessage.dto.request;

public record ChatMessageSendRequest(
        Long roomId,
        String content,
        Long replyMessageId
) {
}
