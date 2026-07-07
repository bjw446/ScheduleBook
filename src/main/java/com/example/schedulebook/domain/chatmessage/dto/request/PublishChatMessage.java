package com.example.schedulebook.domain.chatmessage.dto.request;

import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;

public record PublishChatMessage(
        ChatMessage chatMessage,
        int unreadCount
) {
}
