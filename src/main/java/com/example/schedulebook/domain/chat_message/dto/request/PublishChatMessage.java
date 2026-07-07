package com.example.schedulebook.domain.chat_message.dto.request;

import com.example.schedulebook.domain.chat_message.entity.ChatMessage;

public record PublishChatMessage(
        ChatMessage chatMessage,
        int unreadCount
) {
}
