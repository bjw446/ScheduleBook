package com.example.schedulebook.domain.chat.projection;

import com.example.schedulebook.domain.chat.enums.ChatRoomType;

import java.time.LocalDateTime;

public interface ChatRoomListProjection {
    Long getRoomId();

    String getRoomName();

    String getOpponentNickname();

    String getLastMessage();

    LocalDateTime getLastMessageAt();

    int getUnreadCount();

    ChatRoomType getChatRoomType();
}
