package com.example.schedulebook.domain.chat_room.dto.response;

import com.example.schedulebook.domain.chat_room.entity.ChatRoom;
import com.example.schedulebook.domain.chat_room.enums.ChatRoomType;

public record ChatRoomResponse(
        Long roomId,
        ChatRoomType chatRoomType,
        String roomName,
        int memberCount
) {
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getChatRoomType(),
                chatRoom.getName(),
                chatRoom.getMemberCount()
        );
    }
}
