package com.example.schedulebook.domain.chat.dto.response;

import com.example.schedulebook.domain.chat.entity.ChatRoom;
import com.example.schedulebook.domain.chat.entity.ChatRoomMember;
import com.example.schedulebook.domain.chat.enums.ChatRoomType;

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomDetailResponse(
        Long roomId,
        String roomName,
        ChatRoomType chatRoomType,
        int memberCount,
        Long lastReadMessageId,
        LocalDateTime joinedAt,
        Long lastMessageId,
        String lastMessage,
        LocalDateTime lastMessageAt,
        int unreadCount,
        List<MemberReadStatusResponse> readStatuses
) {
    public static ChatRoomDetailResponse from(
            ChatRoom chatRoom,
            ChatRoomMember chatRoomMember,
            String roomName,
            List<MemberReadStatusResponse> readStatuses
    ) {
        return new ChatRoomDetailResponse(
                chatRoom.getId(),
                roomName,
                chatRoom.getChatRoomType(),
                chatRoom.getMemberCount(),
                chatRoomMember.getLastReadMessageId(),
                chatRoomMember.getJoinedAt(),
                chatRoom.getLastMessage() == null ? null : chatRoom.getLastMessage().getId(),
                chatRoom.getLastMessage() == null ? null : chatRoom.getLastMessage().getContent(),
                chatRoom.getLastMessage() == null ? null : chatRoom.getLastMessage().getCreatedAt(),
                chatRoomMember.getUnreadCount(),
                readStatuses
        );
    }
}
