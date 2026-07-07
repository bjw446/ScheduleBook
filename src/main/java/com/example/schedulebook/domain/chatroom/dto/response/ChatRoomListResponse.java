package com.example.schedulebook.domain.chatroom.dto.response;

import com.example.schedulebook.domain.chatroom.enums.ChatRoomType;
import com.example.schedulebook.domain.chatroom.projection.ChatRoomListProjection;

import java.time.LocalDateTime;

public record ChatRoomListResponse(
        Long roomId,
        String roomName,
        String lastMessage,
        LocalDateTime lastMessageAt,
        int unreadCount
) {
    public static ChatRoomListResponse from(ChatRoomListProjection chatRoomListProjection) {
        return new ChatRoomListResponse(
                chatRoomListProjection.getRoomId(),
                chatRoomListProjection.getChatRoomType() == ChatRoomType.DIRECT
                        ? chatRoomListProjection.getOpponentNickname()
                        : chatRoomListProjection.getRoomName(),
                chatRoomListProjection.getLastMessage(),
                chatRoomListProjection.getLastMessageAt(),
                chatRoomListProjection.getUnreadCount()
        );
    }
}
