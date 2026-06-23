package com.example.schedulebook.domain.chat.dto.response;

import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.enums.ChatMessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String content,
        ChatMessageType chatMessageType,
        Long replyMessageId,
        boolean edited,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChatRoom().getId(),
                chatMessage.getSender().getId(),
                chatMessage.getSender().getNickname(),
                chatMessage.getContent(),
                chatMessage.getChatMessageType(),
                chatMessage.getReplyMessage() == null ? null : chatMessage.getReplyMessage().getId(),
                chatMessage.isEdited(),
                chatMessage.getCreatedAt()
        );
    }
}
