package com.example.schedulebook.domain.chat.dto.response;

import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.enums.ChatMessageType;
import com.example.schedulebook.domain.user.entity.User;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String content,
        ChatMessageType chatMessageType,
        ReplyMessageResponse replyMessageResponse,
        boolean edited,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        User sender = chatMessage.getSender();

        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChatRoom().getId(),
                chatMessage.getSender() == null ? null : sender.getId(),
                chatMessage.getSender() == null ? null : sender.getNickname(),
                chatMessage.getContent(),
                chatMessage.getChatMessageType(),
                chatMessage.getReplyMessage() == null ? null : ReplyMessageResponse.from(chatMessage.getReplyMessage()),
                chatMessage.isEdited(),
                chatMessage.getCreatedAt()
        );
    }
}
