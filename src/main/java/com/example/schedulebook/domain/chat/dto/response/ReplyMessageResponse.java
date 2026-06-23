package com.example.schedulebook.domain.chat.dto.response;

import com.example.schedulebook.domain.chat.entity.ChatMessage;

public record ReplyMessageResponse(
        Long messageId,
        Long senderId,
        String senderNickname,
        String content
) {
    public static ReplyMessageResponse from(ChatMessage chatMessage) {
        return new ReplyMessageResponse(
                chatMessage.getId(),
                chatMessage.getSender().getId(),
                chatMessage.getSender().getNickname(),
                chatMessage.getContent()
        );
    }
}
