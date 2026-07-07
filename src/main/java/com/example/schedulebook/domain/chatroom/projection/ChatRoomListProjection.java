package com.example.schedulebook.domain.chatroom.projection;

import com.example.schedulebook.domain.chatroom.enums.ChatRoomType;

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
