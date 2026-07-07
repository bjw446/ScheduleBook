package com.example.schedulebook.domain.chatmessage.dto.response;

import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.user.entity.User;

public record ReplyMessageResponse(
        Long messageId,
        Long senderId,
        String senderNickname,
        String content
) {
    public static ReplyMessageResponse from(ChatMessage chatMessage) {
        User sender = chatMessage.getSender();

        return new ReplyMessageResponse(
                chatMessage.getId(),
                sender == null ? null : sender.getId(),
                sender == null ? null : sender.getNickname(),
                chatMessage.getContent()
        );
    }
}
