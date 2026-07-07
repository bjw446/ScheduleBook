package com.example.schedulebook.domain.chat_room.projection;

import com.example.schedulebook.domain.chat_room.enums.ChatRoomType;

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
