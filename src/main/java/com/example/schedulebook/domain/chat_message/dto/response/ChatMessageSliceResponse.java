package com.example.schedulebook.domain.chat_message.dto.response;

import java.util.List;

public record ChatMessageSliceResponse(
        List<ChatMessageResponse> messages,
        Long nextCursor,
        boolean hasNext
) {
}
