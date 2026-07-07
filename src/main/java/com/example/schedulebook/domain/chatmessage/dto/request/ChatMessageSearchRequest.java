package com.example.schedulebook.domain.chatmessage.dto.request;

public record ChatMessageSearchRequest(
        Long cursor,
        Integer size
) {
}
