package com.example.schedulebook.domain.chatroom.dto.response;

import java.util.List;

public record ChatRoomSliceResponse(
        List<ChatRoomListResponse> chatRoomListResponses,
        ChatRoomCursor nextCursor,
        boolean hasNext
) {
}
