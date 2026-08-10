package com.example.schedulebook.domain.chatroom.dto.response;

import java.time.LocalDateTime;

public record ChatRoomCursor(
        LocalDateTime lastMessageAt,
        Long roomId
) {
}
