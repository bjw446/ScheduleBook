package com.example.schedulebook.domain.chat.dto.request;

import com.example.schedulebook.domain.chat.entity.ChatMessage;

public record PublishChatMessage(
        ChatMessage chatMessage,
        int unreadCount
) {
}
