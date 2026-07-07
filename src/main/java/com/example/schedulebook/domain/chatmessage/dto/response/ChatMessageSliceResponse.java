package com.example.schedulebook.domain.chatmessage.dto.response;

import java.util.List;

public record ChatMessageSliceResponse(
        List<ChatMessageResponse> messages,
        Long nextCursor,
        boolean hasNext
) {
}
